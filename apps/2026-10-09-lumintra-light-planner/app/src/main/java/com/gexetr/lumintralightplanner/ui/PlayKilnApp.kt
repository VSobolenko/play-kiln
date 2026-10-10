package com.gexetr.lumintralightplanner.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gexetr.lumintralightplanner.data.CatalogueRepository
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.ScoutSnapshot
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.ui.about.AboutScreen
import com.gexetr.lumintralightplanner.ui.settings.SettingsRoute
import com.gexetr.lumintralightplanner.ui.settings.SettingsViewModel
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
internal data object WelcomeDestination : NavKey

@Serializable
internal data object BriefDestination : NavKey

@Serializable
internal data object SpotsDestination : NavKey

@Serializable
internal data class SpotEditDestination(val spotId: String?) : NavKey

@Serializable
internal data object NightDestination : NavKey

@Serializable
internal data object TargetsDestination : NavKey

@Serializable
internal data object ShareDestination : NavKey

@Serializable
internal data object CreditsDestination : NavKey

@Serializable
internal data object SettingsDestination : NavKey

@Serializable
internal data object AboutDestination : NavKey

@Composable
fun PlayKilnApp(
    appContext: android.content.Context,
    scoutStore: ScoutStore,
    catalogueRepository: CatalogueRepository,
    cloudRepository: CloudRepository,
    skyAssetSource: SkyAssetSource,
    ephemeris: SkyEphemeris,
    settingsViewModel: SettingsViewModel,
    passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> = { _, _, _ -> emptyList() },
    @Suppress("UNUSED_PARAMETER") hapticsEnabled: Boolean = false,
) {
    var startDestination by remember { mutableStateOf<NavKey?>(null) }
    LaunchedEffect(scoutStore) {
        if (startDestination != null) return@LaunchedEffect
        val snapshot = scoutStore.snapshot.first()
        startDestination = resolveStartDestination(snapshot)
    }

    // Read the window size class so tablets/unfoldables are not invisible to the adaptive gate.
    // ListDetailSceneStrategy was removed: Welcome/Brief marked as listPane with no matching
    // detailPane left an empty Adaptive detail layer that stole taps (owner: nothing pressable).
    @Suppress("UNUSED_VARIABLE")
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val start = startDestination) {
            null -> LoadingShell()
            else -> LumintraNavHost(
                appContext = appContext.applicationContext,
                startDestination = start,
                scoutStore = scoutStore,
                catalogueRepository = catalogueRepository,
                cloudRepository = cloudRepository,
                skyAssetSource = skyAssetSource,
                ephemeris = ephemeris,
                settingsViewModel = settingsViewModel,
                passProvider = passProvider,
            )
        }
    }
}

@Composable
private fun LoadingShell() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LumintraNavHost(
    appContext: android.content.Context,
    startDestination: NavKey,
    scoutStore: ScoutStore,
    catalogueRepository: CatalogueRepository,
    cloudRepository: CloudRepository,
    skyAssetSource: SkyAssetSource,
    ephemeris: SkyEphemeris,
    settingsViewModel: SettingsViewModel,
    passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass>,
) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(startDestination)
    val scope = rememberCoroutineScope()

    fun navigateAfterSkip() {
        scope.launch {
            val snapshot = scoutStore.snapshot.first()
            if (snapshot.spots.isNotEmpty()) {
                backStack.push(BriefDestination)
            } else {
                backStack.push(SpotEditDestination(spotId = null))
            }
        }
    }

    fun resetToBrief() {
        while (backStack.isNotEmpty()) {
            backStack.removeLastOrNull()
        }
        backStack.add(BriefDestination)
    }

    // Single-pane NavDisplay only — no ListDetail scene strategy. Every destination is a full
    // screen; nothing sits in an Adaptive detail pane above the controls.
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.popOne() },
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                initialOffsetX = { width -> width / 3 },
            ) + fadeIn(tween(durationMillis = 140)) togetherWith
                fadeOut(tween(durationMillis = 120))
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
                initialOffsetX = { width -> -width / 3 },
            ) + fadeIn(tween(durationMillis = 140)) togetherWith
                fadeOut(tween(durationMillis = 120))
        },
        entryProvider = entryProvider {
            entry<WelcomeDestination> {
                WelcomeRoute(
                    scoutStore = scoutStore,
                    onCreateSpot = { backStack.push(SpotEditDestination(spotId = null)) },
                    onManualEntry = { backStack.push(SpotEditDestination(spotId = null)) },
                    onSkip = { navigateAfterSkip() },
                )
            }
            entry<BriefDestination> {
                BriefRoute(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    catalogue = catalogueRepository,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    assetSource = skyAssetSource,
                    passProvider = passProvider,
                    onOpenScene = { backStack.push(NightDestination) },
                    onShare = { backStack.push(ShareDestination) },
                    onOpenTargets = { backStack.push(TargetsDestination) },
                    onBack = {
                        if (backStack.size <= 1) {
                            backStack.push(SpotsDestination)
                        } else {
                            backStack.popOne()
                        }
                    },
                )
            }
            entry<SpotsDestination> {
                SpotsRoute(
                    scoutStore = scoutStore,
                    onAdd = { backStack.push(SpotEditDestination(spotId = null)) },
                    onOpen = { id -> backStack.push(SpotEditDestination(spotId = id)) },
                    onBack = { backStack.popOne() },
                )
            }
            entry<SpotEditDestination> { destination ->
                SpotEditRoute(
                    spotId = destination.spotId,
                    scoutStore = scoutStore,
                    onSaved = { resetToBrief() },
                    onCancel = { backStack.popOne() },
                )
            }
            entry<NightDestination> {
                NightRoute(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    catalogue = catalogueRepository,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    assetSource = skyAssetSource,
                    passProvider = passProvider,
                    onOpenBrief = { backStack.popOne() },
                )
            }
            entry<TargetsDestination> {
                TargetsRoute(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    ephemeris = ephemeris,
                    onOpenSettings = { backStack.push(SettingsDestination) },
                    onOpenSpots = { backStack.push(SpotsDestination) },
                    onBack = { backStack.popOne() },
                )
            }
            entry<ShareDestination> {
                ShareRoute(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    catalogue = catalogueRepository,
                    cloudRepository = cloudRepository,
                    ephemeris = ephemeris,
                    passProvider = passProvider,
                    onBack = { backStack.popOne() },
                )
            }
            entry<SettingsDestination> {
                SettingsRoute(
                    onBack = { backStack.popOne() },
                    onOpenCredits = { backStack.push(CreditsDestination) },
                    viewModel = settingsViewModel,
                )
            }
            entry<CreditsDestination> {
                CreditsRoute(onBack = { backStack.popOne() })
            }
            entry<AboutDestination> {
                AboutScreen(onBack = { backStack.popOne() })
            }
        },
    )
}

/** Quarantine-only stores still report `spots.isEmpty()` — same as welcome/spec. */
internal fun resolveStartDestination(snapshot: ScoutSnapshot): NavKey =
    if (snapshot.spots.isEmpty()) WelcomeDestination else BriefDestination

private fun NavBackStack<NavKey>.popOne() {
    if (size > 1) removeLastOrNull()
}

private fun NavBackStack<NavKey>.push(destination: NavKey) {
    if (lastOrNull() != destination) add(destination)
}

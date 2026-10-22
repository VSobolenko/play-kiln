package com.gexetr.coverkit.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gexetr.coverkit.R
import com.gexetr.coverkit.data.BrandKitStore
import com.gexetr.coverkit.data.ProjectStore
import com.gexetr.coverkit.domain.TemplateFactory
import com.gexetr.coverkit.ui.about.AboutScreen
import com.gexetr.coverkit.ui.brand.BrandScreen
import com.gexetr.coverkit.ui.destinations.DestinationsScreen
import com.gexetr.coverkit.ui.editor.EditorScreen
import com.gexetr.coverkit.ui.projects.ProjectsScreen
import com.gexetr.coverkit.ui.settings.SettingsRoute
import com.gexetr.coverkit.ui.templates.TemplatesScreen
import com.gexetr.coverkit.ui.unlock.PurchaseGate
import com.gexetr.coverkit.ui.unlock.UnlockScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data object DestinationsKey : NavKey

@Serializable
private data class TemplatesKey(val destinationId: String) : NavKey

@Serializable
private data class EditorKey(val projectId: String) : NavKey

@Serializable
private data object ProjectsKey : NavKey

@Serializable
private data object BrandKey : NavKey

@Serializable
private data object SettingsKey : NavKey

@Serializable
private data object AboutKey : NavKey

@Serializable
private data object UnlockKey : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PlayKilnApp(
    projectStore: ProjectStore,
    brandKitStore: BrandKitStore,
    purchaseGate: PurchaseGate,
    hapticsEnabled: Boolean = false,
) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(DestinationsKey)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val unlocked by purchaseGate.owned.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // FLAG_SECURE removed for v1: it zeroed adb screencap on the test phone, so the store
    // pack could not photograph the free build. Free Export still watermarks the PNG file.

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.pop() },
            sceneStrategies = listOf(listDetailStrategy),
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally { it / 8 }) togetherWith
                    fadeOut(tween(180))
            },
            popTransitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally { -it / 8 }) togetherWith
                    fadeOut(tween(180))
            },
            entryProvider = entryProvider {
                entry<DestinationsKey>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { DetailPlaceholder() },
                    ),
                ) {
                    DestinationsScreen(
                        onDestination = { id -> backStack.push(TemplatesKey(id)) },
                        onProjects = { backStack.push(ProjectsKey) },
                        onBrand = { backStack.push(BrandKey) },
                        onSettings = { backStack.push(SettingsKey) },
                        onAbout = { backStack.push(AboutKey) },
                        hapticsEnabled = hapticsEnabled,
                    )
                }
                entry<TemplatesKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    TemplatesScreen(
                        destinationId = key.destinationId,
                        unlocked = unlocked,
                        onBack = { backStack.pop() },
                        onOpenProject = { templateId ->
                            scope.launch {
                                val localized = TemplateFactory.nameResOrNull(templateId)
                                    ?.let(context::getString)
                                val project = TemplateFactory.createProject(templateId).let { p ->
                                    if (localized != null) p.copy(name = localized) else p
                                }
                                projectStore.save(project)
                                backStack.push(EditorKey(project.id))
                            }
                        },
                        onNeedUnlock = { backStack.push(UnlockKey) },
                        onCreateBlank = {
                            scope.launch {
                                val project = TemplateFactory.blankProject(
                                    destinationId = key.destinationId,
                                    name = context.getString(R.string.template_blank_photo),
                                )
                                projectStore.save(project)
                                backStack.push(EditorKey(project.id))
                            }
                        },
                    )
                }
                entry<EditorKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    EditorScreen(
                        projectId = key.projectId,
                        projectStore = projectStore,
                        brandKitStore = brandKitStore,
                        unlocked = unlocked,
                        onBack = { backStack.pop() },
                        onNeedUnlock = { backStack.push(UnlockKey) },
                    )
                }
                entry<ProjectsKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    ProjectsScreen(
                        projectStore = projectStore,
                        onBack = { backStack.pop() },
                        onOpen = { id -> backStack.push(EditorKey(id)) },
                    )
                }
                entry<BrandKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    BrandScreen(
                        brandKitStore = brandKitStore,
                        unlocked = unlocked,
                        onBack = { backStack.pop() },
                        onNeedUnlock = { backStack.push(UnlockKey) },
                    )
                }
                entry<SettingsKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsRoute(
                        onBack = { backStack.pop() },
                        purchaseGate = purchaseGate,
                        onUnlock = { backStack.push(UnlockKey) },
                    )
                }
                entry<AboutKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    AboutScreen(onBack = { backStack.pop() })
                }
                entry<UnlockKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    UnlockScreen(
                        gate = purchaseGate,
                        onBack = { backStack.pop() },
                    )
                }
            },
        )
    }
}

private fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeLastOrNull()
}

private fun NavBackStack<NavKey>.push(destination: NavKey) {
    if (lastOrNull() != destination) add(destination)
}

@Composable
private fun DetailPlaceholder() {
    Text(
        text = stringResource(R.string.home_pick_section),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

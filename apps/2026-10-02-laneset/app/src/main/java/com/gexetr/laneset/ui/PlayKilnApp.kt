package com.gexetr.laneset.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gexetr.laneset.R
import com.gexetr.laneset.ui.about.AboutScreen
import com.gexetr.laneset.ui.home.HomeRoute
import com.gexetr.laneset.ui.importer.CoachImportRoute
import com.gexetr.laneset.ui.pace.PaceTestRoute
import com.gexetr.laneset.ui.settings.SettingsRoute
import com.gexetr.laneset.ui.unlock.UnlockRoute
import kotlinx.serialization.Serializable

/**
 * The destinations, as Navigation 3 keys.
 *
 * `@Serializable` and `NavKey` are what `rememberNavBackStack` requires: it stores the back stack
 * through Navigation 3's own serializer rather than a hand-rolled `listSaver`. The host owning the
 * back stack is what keeps `NavDisplay`'s transitions, predictive-back continuity and scene
 * strategies live — see `.cursor/rules/ui-ux-baseline.mdc`.
 *
 * Note the library's exact shape, because it is not the one a hand-rolled back stack suggests:
 * `rememberNavBackStack` is **not** generic and returns `NavBackStack<NavKey>` (Navigation 3 1.1.7;
 * Google's own recipes only get a narrower element type by declaring their own overload — see
 * `recipes/conditional.md` in the vendored Navigation 3 skill). So the entries below are typed by the
 * `entryProvider` DSL rather than by a `when` over the stack: each `entry<T>` states its own key type,
 * which is what keeps a destination's payload strongly typed without an `else` branch swallowing a
 * case nobody handled.
 */
@Serializable
private data object HomeDestination : NavKey

@Serializable
private data object AboutDestination : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Serializable
private data object PaceTestDestination : NavKey

@Serializable
private data object CoachImportDestination : NavKey

@Serializable
private data object UnlockDestination : NavKey

/**
 * The app shell.
 *
 * The list-detail strategy is what makes the form factor matter. Below its own breakpoint Home fills
 * the window exactly as it always did; above it, Home stays in the leading pane while the section it
 * opens appears beside it instead of replacing it. Without a strategy the same back stack renders one
 * stretched column on a tablet, which is the defect this rule exists for.
 *
 * [hapticsEnabled] is passed down rather than read here: the preference is owned by the settings store
 * and hoisted by the activity, so the shell stays a function of its arguments and its previews need no
 * DataStore.
 *
 * Home is the only list pane; every other destination is a detail pane pushed on the same host-owned
 * stack, so Home stays beside whichever flow is open on a wide window and predictive back pops the
 * flow like any other destination.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PlayKilnApp(hapticsEnabled: Boolean = false) {
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeDestination)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    // Without this Surface nothing between the window and `NavDisplay` provides a content colour, so
    // `LocalContentColor` keeps Compose's default `Color.Black` and every default-coloured Text/Icon
    // is invisible when the window background is dark. `Surface` sets
    // `contentColor = contentColorFor(containerColor)`, and this `background` yields `onBackground`.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.pop() },
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<HomeDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = { DetailPlaceholder() },
                    ),
                ) {
                    HomeRoute(
                        onNavigateToAbout = { backStack.push(AboutDestination) },
                        onNavigateToSettings = { backStack.push(SettingsDestination) },
                        onNavigateToPaceTest = { backStack.push(PaceTestDestination) },
                        onNavigateToCoachImport = { backStack.push(CoachImportDestination) },
                        onNavigateToUnlock = { backStack.push(UnlockDestination) },
                        hapticsEnabled = hapticsEnabled,
                    )
                }
                entry<AboutDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    AboutScreen(onBack = { backStack.pop() })
                }
                entry<SettingsDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    SettingsRoute(onBack = { backStack.pop() })
                }
                entry<PaceTestDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    PaceTestRoute(onBack = { backStack.pop() })
                }
                entry<CoachImportDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    CoachImportRoute(onBack = { backStack.pop() })
                }
                entry<UnlockDestination>(metadata = ListDetailSceneStrategy.detailPane()) {
                    UnlockRoute(onBack = { backStack.pop() })
                }
            },
        )
    }
}

/** One pop, and only when there is something to pop. */
private fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeLastOrNull()
}

/** One push, and never a duplicate of the destination already on top. */
private fun NavBackStack<NavKey>.push(destination: NavKey) {
    if (lastOrNull() != destination) add(destination)
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

package com.gexetr.smoke.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gexetr.smoke.ui.about.AboutScreen
import com.gexetr.smoke.ui.home.HomeRoute

private sealed interface PlayKilnDestination

private data object HomeDestination : PlayKilnDestination

private data object AboutDestination : PlayKilnDestination

private const val HomeDestinationKey = "home"
private const val AboutDestinationKey = "about"

private val DestinationBackStackSaver =
    listSaver<SnapshotStateList<PlayKilnDestination>, String>(
        save = { backStack ->
            backStack.map { destination ->
                when (destination) {
                    HomeDestination -> HomeDestinationKey
                    AboutDestination -> AboutDestinationKey
                }
            }
        },
        restore = { savedKeys ->
            mutableStateListOf<PlayKilnDestination>().apply {
                savedKeys.mapTo(this) { key ->
                    when (key) {
                        AboutDestinationKey -> AboutDestination
                        else -> HomeDestination
                    }
                }
                if (isEmpty()) add(HomeDestination)
            }
        },
    )

@Composable
fun PlayKilnApp() {
    val backStack = rememberSaveable(saver = DestinationBackStackSaver) {
        mutableStateListOf<PlayKilnDestination>(HomeDestination)
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = { key ->
            when (key) {
                HomeDestination -> NavEntry(key) {
                    HomeRoute(
                        onNavigateToAbout = {
                            if (backStack.lastOrNull() != AboutDestination) {
                                backStack.add(AboutDestination)
                            }
                        },
                    )
                }
                AboutDestination -> NavEntry(key) {
                    AboutScreen(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                    )
                }
            }
        },
    )
}

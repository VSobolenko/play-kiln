package com.gexetr.laneset

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.laneset.data.AppGraph
import com.gexetr.laneset.ui.PlayKilnApp
import com.gexetr.laneset.ui.settings.SettingsViewModel
import com.gexetr.laneset.ui.theme.PlayKilnTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * The single activity, and the one place the app's graph and its billing lifecycle are owned.
 *
 * Two duties beyond hosting the shell:
 *
 * - **Building the graph.** [AppGraph] holds `lateinit` stores and one `BillingManager`; before this
 *   change nothing called `AppGraph.init`, so the first screen to read a store would have crashed on
 *   an uninitialised `lateinit`. It is built here, in `onCreate`, before the first composition — and
 *   because `init` is idempotent it is safe on every recreation.
 * - **Owning the billing lifecycle for the foreground.** The Play `BillingClient` is connected while
 *   the app is visible and dropped the moment it is not, so a process that is only showing a
 *   notification is not holding a store connection open. [AppGraph.billing] is the same instance the
 *   Unlock screen reads, so the entitlement and availability it shows are the ones this lifecycle
 *   resolved.
 *
 * The settings are still hoisted here rather than inside the theme or the shell, because the theme is
 * applied *outside* the navigation host and cannot read a destination's state. `themeMode` and
 * `dynamicColor` decide which palette composes; `hapticsEnabled` is handed to the shell, which gives
 * it to the screen whose action it changes.
 */
class MainActivity : ComponentActivity() {

    /**
     * Runs the connect/disconnect pair, so a stop can cancel a connect that has not settled yet — a
     * quick background must not leave a completed connection behind a `disconnect()`.
     */
    private val billingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before the first composition: a `lateinit` the first screen reads must already be set.
        AppGraph.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(applicationContext))
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            PlayKilnTheme(
                darkTheme = settings.themeMode.isDark(systemInDarkTheme),
                dynamicColor = settings.dynamicColor,
            ) {
                PlayKilnApp(hapticsEnabled = settings.hapticsEnabled)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        billingScope.launch { AppGraph.billing.connect() }
    }

    override fun onStop() {
        // Cancel an in-flight connect first, so it cannot complete after the disconnect below.
        billingScope.coroutineContext.cancelChildren()
        AppGraph.billing.disconnect()
        super.onStop()
    }

    override fun onDestroy() {
        billingScope.cancel()
        super.onDestroy()
    }
}

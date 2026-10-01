package com.gexetr.scaffold

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.scaffold.ui.PlayKilnApp
import com.gexetr.scaffold.ui.settings.SettingsViewModel
import com.gexetr.scaffold.ui.theme.PlayKilnTheme

/**
 * The single activity, and the one place the persisted settings are read for the whole app.
 *
 * The settings are hoisted here rather than inside the theme or the shell, because the theme is
 * applied *outside* the navigation host and cannot read a destination's state. `themeMode` and
 * `dynamicColor` decide which palette composes; `hapticsEnabled` is handed to the shell, which gives
 * it to the screen whose action it changes. Every one of them is a preference the user chose in
 * Settings, which is what makes that screen more than decoration.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

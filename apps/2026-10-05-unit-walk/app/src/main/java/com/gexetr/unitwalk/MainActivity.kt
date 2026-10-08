package com.gexetr.unitwalk

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.unitwalk.ui.UnitWalkApp
import com.gexetr.unitwalk.ui.settings.SettingsViewModel
import com.gexetr.unitwalk.ui.theme.UnitWalkTheme

/**
 * The single activity, and the one place the persisted settings are read for the whole app.
 *
 * Extends [AppCompatActivity] rather than a bare `ComponentActivity` so the in-app language picker
 * can call `AppCompatDelegate.setApplicationLocales` — the published caution is that the call only
 * takes effect when its host is an `AppCompatActivity`. `AppCompatActivity` is a `ComponentActivity`,
 * so `enableEdgeToEdge()` and `setContent` are unaffected.
 *
 * The settings are hoisted here rather than inside the theme or the shell, because the theme is
 * applied *outside* the navigation host and cannot read a destination's state. `themeMode` and
 * `dynamicColor` decide which palette composes; `hapticsEnabled` is handed to the shell, which spends
 * it on the one act the shell itself performs — a screen change (see `UnitWalkApp`).
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(applicationContext))
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            UnitWalkTheme(
                darkTheme = settings.themeMode.isDark(systemInDarkTheme),
                dynamicColor = settings.dynamicColor,
            ) {
                UnitWalkApp(hapticsEnabled = settings.hapticsEnabled)
            }
        }
    }
}

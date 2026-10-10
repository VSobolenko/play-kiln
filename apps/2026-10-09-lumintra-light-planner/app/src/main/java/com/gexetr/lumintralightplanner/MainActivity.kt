package com.gexetr.lumintralightplanner

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.lumintralightplanner.data.CatalogueRepository
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import com.gexetr.lumintralightplanner.data.sky.AstronomySkyEphemeris
import com.gexetr.lumintralightplanner.ui.PlayKilnApp
import com.gexetr.lumintralightplanner.ui.night.SceneMemoryPressure
import com.gexetr.lumintralightplanner.ui.settings.SettingsViewModel
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme

/**
 * The single activity, and the one place the persisted settings are read for the whole app.
 *
 * Extends [AppCompatActivity] rather than a bare `ComponentActivity` so the in-app language picker
 * can call `AppCompatDelegate.setApplicationLocales` — the published caution is that the call only
 * takes effect when its host is an `AppCompatActivity`. `AppCompatActivity` is a `ComponentActivity`,
 * so `enableEdgeToEdge()` and `setContent` are unaffected.
 *
 * The settings are hoisted here rather than inside the theme or the shell, because the theme is
 * applied *outside* the navigation host and cannot read a destination's state. Repositories and the
 * scout store are created once here and passed into the shell so every destination reads the same
 * on-disk plan.
 *
 * [onTrimMemory] forwards to [SceneMemoryPressure] so the night scene's LUT strips and Milky Way
 * shader unbind even if the [LumintraApplication] path is skipped on a given host.
 */
class MainActivity : AppCompatActivity() {

    private val memoryCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) = SceneMemoryPressure.onTrimMemory(level)
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        override fun onLowMemory() =
            SceneMemoryPressure.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SceneMemoryPressure.install(applicationContext)
        registerComponentCallbacks(memoryCallbacks)
        enableEdgeToEdge()
        setContent {
            val appContext = applicationContext
            val scoutStore = remember { ScoutStore(appContext) }
            val catalogueRepository = remember { CatalogueRepository(appContext) }
            val cloudRepository = remember { CloudRepository(appContext) }
            val skyAssetSource = remember { SkyAssetSource(appContext) }
            val ephemeris = remember { AstronomySkyEphemeris() }

            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(appContext))
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            PlayKilnTheme(
                darkTheme = settings.themeMode.isDark(systemInDarkTheme),
                dynamicColor = settings.dynamicColor,
            ) {
                PlayKilnApp(
                    appContext = appContext,
                    scoutStore = scoutStore,
                    catalogueRepository = catalogueRepository,
                    cloudRepository = cloudRepository,
                    skyAssetSource = skyAssetSource,
                    ephemeris = ephemeris,
                    settingsViewModel = settingsViewModel,
                    hapticsEnabled = settings.hapticsEnabled,
                )
            }
        }
    }

    override fun onDestroy() {
        unregisterComponentCallbacks(memoryCallbacks)
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        SceneMemoryPressure.onTrimMemory(level)
    }
}

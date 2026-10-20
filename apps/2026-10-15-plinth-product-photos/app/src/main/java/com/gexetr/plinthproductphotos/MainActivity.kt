package com.gexetr.plinthproductphotos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gexetr.plinthproductphotos.ui.PlayKilnApp
import com.gexetr.plinthproductphotos.ui.settings.SettingsViewModel
import com.gexetr.plinthproductphotos.ui.theme.PlayKilnTheme
import com.gexetr.plinthproductphotos.data.session.PlayIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.DevIntegrityTokenProvider
import com.gexetr.plinthproductphotos.data.session.IntegrityTokenProvider
import com.gexetr.plinthproductphotos.ui.cutout.CutoutViewModel
import com.gexetr.plinthproductphotos.ui.session.SessionStatusBanner

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
 * decides light or dark of the slot's own palette; `hapticsEnabled` is handed to the shell, which gives
 * it to the screen whose action it changes.
 */
class MainActivity : AppCompatActivity() {
    private val sessionViewModel by lazy {
        ViewModelProvider(this, CutoutViewModel.factory(applicationContext))[CutoutViewModel::class.java]
    }
    private val integrity: IntegrityTokenProvider by lazy {
        if (BuildConfig.DEBUG) {
            DevIntegrityTokenProvider()
        } else {
            PlayIntegrityTokenProvider(applicationContext, BuildConfig.INTEGRITY_CLOUD_PROJECT_NUMBER)
        }
    }

    override fun onStart() {
        super.onStart()
        sessionViewModel.createServerSession(integrity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.factory(applicationContext))
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val connection by sessionViewModel.sessionConnection.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            PlayKilnTheme(
                darkTheme = settings.themeMode.isDark(systemInDarkTheme),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        PlayKilnApp(hapticsEnabled = settings.hapticsEnabled)
                    }
                    SessionStatusBanner(
                        state = connection,
                        onRetry = { sessionViewModel.createServerSession(integrity) },
                    )
                }
            }
        }
    }
}

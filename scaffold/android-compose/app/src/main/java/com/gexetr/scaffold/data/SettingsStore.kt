package com.gexetr.scaffold.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * How the app chooses between its light and dark palettes.
 *
 * [FOLLOW_SYSTEM] is deliberately the default and the first option in the settings list: the
 * platform's day/night setting is what the user has already told the device, and an app that
 * overrides it without being asked is the app that ignores that answer. The two explicit modes exist
 * for the user who wants this app dark while the device is light, which is a real and common ask.
 */
enum class ThemeMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK;

    /** Resolves the stored choice against the platform's own night-mode answer. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        FOLLOW_SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }
}

/**
 * Everything the user can change, as one immutable value.
 *
 * The default of each field is the *conservative* one — follow the system, keep the platform's
 * Material You colours, keep feedback on — so a user who never opens Settings gets an app that
 * behaves the way the platform expects. These defaults are also what the settings screen shows
 * before the store has been read, and what a DataStore read failure falls back to.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

/**
 * The settings store, as a stream of values plus the writes that change them.
 *
 * An interface rather than the DataStore type itself, so the ViewModel and the screen never name
 * `DataStore` and a test or preview can supply an in-memory implementation.
 */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setHapticsEnabled(enabled: Boolean)
}

/**
 * The single DataStore instance for the app.
 *
 * `preferencesDataStore` is a property delegate: declaring it more than once for the same name
 * throws at runtime, which is the failure the delegate exists to prevent. It is private to this file
 * so the only route to the file on disk is through [DataStoreSettingsStore].
 */
private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * The DataStore-backed implementation of [SettingsStore].
 *
 * The published pattern is followed exactly, because each line of it is a defect that was measured
 * somewhere else:
 *
 * - the read is `catch`ed and an [IOException] becomes [emptyPreferences], so a corrupt or
 *   unreadable file degrades to the defaults instead of throwing into the composition;
 * - only preferences the user has actually changed are written, so "never opened Settings" is
 *   distinguishable on disk from "explicitly chose the default";
 * - the enum is stored by `name`, and an unrecognised value falls back to the default rather than
 *   crashing, so a downgrade cannot brick the screen.
 */
class DataStoreSettingsStore(private val context: Context) : SettingsStore {

    override val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE] }
                    ?: ThemeMode.FOLLOW_SYSTEM,
                dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[HAPTICS_ENABLED] = enabled }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }
}

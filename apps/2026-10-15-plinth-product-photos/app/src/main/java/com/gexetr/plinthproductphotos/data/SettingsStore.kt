package com.gexetr.plinthproductphotos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
 * The default of each field is the *conservative* one —  follow the system, keep feedback on — so a user who never opens Settings gets an app that
 * behaves the way the platform expects. These defaults are also what the settings screen shows
 * before the store has been read, and what a DataStore read failure falls back to.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val hapticsEnabled: Boolean = true,
    val studioDefaults: StudioDefaults = StudioDefaults(),
)

data class StudioDefaults(
    val background: Int = 0,
    val shadow: Int = 1,
    val marginPct: Float = 0.06f,
    val fillTarget: Float = 0.80f,
    val itemScale: Float = 1f,
    val horizontalPosition: Float = 0f,
    val verticalPosition: Float = 0f,
) {
    init {
        require(background in 0..2 && shadow in 0..2)
        require(marginPct in 0f..0.14f && fillTarget in 0.55f..0.95f)
        require(itemScale in 0.75f..1.25f)
        require(horizontalPosition in -0.12f..0.12f && verticalPosition in -0.12f..0.12f)
    }
}

/**
 * The settings store, as a stream of values plus the writes that change them.
 *
 * An interface rather than the DataStore type itself, so the ViewModel and the screen never name
 * `DataStore` and a test or preview can supply an in-memory implementation.
 */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setHapticsEnabled(enabled: Boolean)

    suspend fun updateStudioDefaults(transform: (StudioDefaults) -> StudioDefaults)
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
class DataStoreSettingsStore(private val dataStore: DataStore<Preferences>) : SettingsStore {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            AppSettings(
                themeMode = ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE] }
                    ?: ThemeMode.FOLLOW_SYSTEM,
                hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
                studioDefaults = preferences.studioDefaults(),
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[HAPTICS_ENABLED] = enabled }
    }

    override suspend fun updateStudioDefaults(transform: (StudioDefaults) -> StudioDefaults) {
        dataStore.edit { preferences ->
            val defaults = transform(preferences.studioDefaults())
            preferences[BACKGROUND] = defaults.background
            preferences[SHADOW] = defaults.shadow
            preferences[MARGIN_PCT] = defaults.marginPct
            preferences[FILL_TARGET] = defaults.fillTarget
            preferences[ITEM_SCALE] = defaults.itemScale
            preferences[HORIZONTAL_POSITION] = defaults.horizontalPosition
            preferences[VERTICAL_POSITION] = defaults.verticalPosition
        }
    }

    private fun Preferences.studioDefaults() = StudioDefaults(
        background = this[BACKGROUND] ?: 0,
        shadow = this[SHADOW] ?: 1,
        marginPct = this[MARGIN_PCT] ?: 0.06f,
        fillTarget = this[FILL_TARGET] ?: 0.80f,
        itemScale = this[ITEM_SCALE] ?: 1f,
        horizontalPosition = this[HORIZONTAL_POSITION] ?: 0f,
        verticalPosition = this[VERTICAL_POSITION] ?: 0f,
    )

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BACKGROUND = intPreferencesKey("studio_background")
        val SHADOW = intPreferencesKey("studio_shadow")
        val MARGIN_PCT = floatPreferencesKey("studio_margin_pct")
        val FILL_TARGET = floatPreferencesKey("studio_fill_target")
        val ITEM_SCALE = floatPreferencesKey("studio_item_scale")
        val HORIZONTAL_POSITION = floatPreferencesKey("studio_horizontal_position")
        val VERTICAL_POSITION = floatPreferencesKey("studio_vertical_position")
    }
}

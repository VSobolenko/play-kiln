package com.gexetr.laneset.data

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
 * Each field's default is the *conservative* one — follow the platform's day/night setting, keep the
 * platform's Material You colours, keep haptic feedback on — so a user who never opens Settings gets
 * an app that behaves the way the platform expects. The defaults are also what the settings screen
 * renders before the store's first emission, and what a DataStore read failure falls back to.
 *
 * **The set is exactly the preferences a real code path reads, and it is kept that way on purpose.**
 * `scripts/core/settings.mjs` (`settings.behaviour`) fails a slot that declares a preference read
 * nowhere outside the settings surface and the data layer, because a toggle that changes nothing is
 * theatre; a field added here before the screen that reads it exists is therefore a gate failure, not
 * foresight. So the list grows one entry at a time, each added together with its consumer:
 *
 * | field | read by |
 * |---|---|
 * | [themeMode] | `MainActivity` -> `PlayKilnTheme(darkTheme = …)` |
 * | [dynamicColor] | `MainActivity` -> `PlayKilnTheme(dynamicColor = …)` |
 * | [hapticsEnabled] | `MainActivity` -> `PlayKilnApp(…)` -> `HomeScreen`'s haptic feedback call |
 *
 * There is deliberately **no language field**. The manifest declares `android:localeConfig`, so
 * Android 13+ already owns a per-app language picker and stores the choice itself; a second stored
 * language tag would be a second writer for one value, and the two could disagree. The settings
 * surface's language row opens the platform picker instead (`.cursor/rules/app-settings.mdc`).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

/**
 * The settings store, as a stream of values plus the writes that change them.
 *
 * An interface rather than the DataStore type itself, so the ViewModel and the screen never name
 * `DataStore` and a test or a preview can supply an in-memory implementation.
 */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setHapticsEnabled(enabled: Boolean)

    companion object {
        /**
         * The pool length the session builder pre-fills when the swimmer has never chosen one.
         *
         * A default is not a preference: it is a value the app supplies, and it belongs here as a
         * constant rather than as an [AppSettings] field precisely because nothing has been built to
         * *read* a swimmer's stored pool length yet — a stored pool length nobody reads would be the
         * dead toggle `settings.behaviour` exists to refuse. Publishing it as a constant gives the
         * builder one place to take its initial value from, so a second copy of `25` cannot appear
         * under `ui/` and drift from this one (`.cursor/rules/verification-discipline.mdc`, "one
         * source of truth, imported — never restated"). When the builder grows a pool-length row,
         * the field, the writer and that reading arrive together.
         */
        const val DEFAULT_POOL_LENGTH_METERS: Int = 25
    }
}

/**
 * The single DataStore instance for the app's preferences.
 *
 * `preferencesDataStore` is a property delegate: declaring it more than once for the same name
 * throws at runtime, which is the failure the delegate exists to prevent. It is private to this file
 * so the only route to the file on disk is through [DataStoreSettingsStore].
 */
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

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
 * - the theme is stored by enum `name`, and an unrecognised value falls back to
 *   [ThemeMode.FOLLOW_SYSTEM] rather than crashing, so a downgrade cannot brick the screen.
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
                dynamicColor = preferences[DYNAMIC_COLOR] ?: false,
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

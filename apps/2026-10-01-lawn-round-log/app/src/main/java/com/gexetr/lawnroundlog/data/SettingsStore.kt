package com.gexetr.lawnroundlog.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.lawnroundlog.ui.AppearanceMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * The single preferences file for this app.
 *
 * `preferencesDataStore` is a property delegate, and declaring it more than once for one name
 * throws at runtime — which is why it is private to this file and why the only route to the file
 * is through [SettingsStore].
 */
private val Context.roundLogSettingsDataStore by preferencesDataStore(name = "round_log_settings")

/**
 * Everything the operator can change, as four streams plus the writes that change them.
 *
 * Each preference is exposed as a [Flow] rather than a snapshot getter because DataStore reads are
 * asynchronous and the UI must redraw when a value changes — a screen that reads the file once and
 * caches the answer is the settings defect that only shows up after someone changes the setting.
 *
 * The defaults are the conservative answer for someone who never opens Settings: the dollar sign
 * that most of the measured shelf quotes in, the round's standard visit price as an editable
 * pre-fill rather than a silent zero, confirmation on before a destructive action, and haptic
 * feedback on. A default is not a stored value: nothing is written until the operator writes it,
 * so "never chose" stays distinguishable from "chose the default".
 *
 * Every one of these is read by a real code path outside this file and the settings screen —
 * [hapticsEnabled] by the round's own controls, [currencySymbol] and [defaultVisitAmountUsd] by the
 * amount shown and pre-filled on a visit, [confirmBeforeDelete] by the delete path. That is the
 * whole point of the settings surface: a preference that nothing reads is decoration.
 */
class SettingsStore(private val context: Context) {

    /** The palette choice the theme host restores after the process returns. */
    val appearance: Flow<AppearanceMode> = preference(APPEARANCE, AppearanceMode.FOLLOW_SYSTEM.name)
        .map { stored -> AppearanceMode.entries.firstOrNull { it.name == stored } ?: AppearanceMode.FOLLOW_SYSTEM }

    /** The symbol prefixed to every amount the operator sees. Stored as text, never as a number. */
    val currencySymbol: Flow<String> = preference(CURRENCY_SYMBOL, DEFAULT_CURRENCY_SYMBOL)

    /** The amount a new visit is pre-filled with, in US dollars, regardless of [currencySymbol]. */
    val defaultVisitAmountUsd: Flow<Double> =
        preference(DEFAULT_VISIT_AMOUNT_USD, DEFAULT_VISIT_AMOUNT_USD_VALUE)

    /** Whether a destructive action asks first and names what it will remove. */
    val confirmBeforeDelete: Flow<Boolean> =
        preference(CONFIRM_BEFORE_DELETE, DEFAULT_CONFIRM_BEFORE_DELETE)

    /** Whether recording a visit gives haptic feedback, which matters outdoors in gloves. */
    val hapticsEnabled: Flow<Boolean> = preference(HAPTICS_ENABLED, DEFAULT_HAPTICS_ENABLED)

    suspend fun setAppearance(value: AppearanceMode) {
        context.roundLogSettingsDataStore.edit { preferences -> preferences[APPEARANCE] = value.name }
    }

    suspend fun setCurrencySymbol(value: String) {
        context.roundLogSettingsDataStore.edit { preferences -> preferences[CURRENCY_SYMBOL] = value }
    }

    suspend fun setDefaultVisitAmountUsd(value: Double) {
        context.roundLogSettingsDataStore.edit { preferences ->
            preferences[DEFAULT_VISIT_AMOUNT_USD] = value
        }
    }

    suspend fun setConfirmBeforeDelete(value: Boolean) {
        context.roundLogSettingsDataStore.edit { preferences ->
            preferences[CONFIRM_BEFORE_DELETE] = value
        }
    }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.roundLogSettingsDataStore.edit { preferences -> preferences[HAPTICS_ENABLED] = value }
    }

    /**
     * One preference as a stream that degrades to [fallback] instead of failing.
     *
     * The `catch` is the published DataStore pattern and it is not optional: an unreadable
     * preferences file throws [IOException] out of the flow, and a flow that throws into the
     * composition takes the screen down. Degrading to [emptyPreferences] makes a corrupt settings
     * file cost the operator his preferences rather than the app.
     */
    private fun <T> preference(key: Preferences.Key<T>, fallback: T): Flow<T> =
        context.roundLogSettingsDataStore.data
            .catch { cause ->
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { preferences -> preferences[key] ?: fallback }

    /**
     * The preference keys stay private. The two defaults the settings surface has to know
     * *synchronously* — before the store's first emission, and therefore before any flow can supply
     * them — are published, because the alternative is a second copy of `"$"` and `40.0` under `ui/`.
     * A duplicated default is a default that drifts, and the drift is invisible: the screen shows one
     * price before it loads and another after
     * (`.cursor/rules/verification-discipline.mdc`, "one source of truth, imported — never restated").
     */
    companion object {
        private val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val DEFAULT_VISIT_AMOUNT_USD = doublePreferencesKey("default_visit_amount_usd")
        private val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val APPEARANCE = stringPreferencesKey("appearance")

        /** The symbol a round shows when the operator has never chosen one. */
        const val DEFAULT_CURRENCY_SYMBOL: String = "$"

        /** The visit price a new visit is pre-filled with when the operator has never chosen one. */
        const val DEFAULT_VISIT_AMOUNT_USD_VALUE: Double = 40.0

        private const val DEFAULT_CONFIRM_BEFORE_DELETE = true
        private const val DEFAULT_HAPTICS_ENABLED = true
    }
}

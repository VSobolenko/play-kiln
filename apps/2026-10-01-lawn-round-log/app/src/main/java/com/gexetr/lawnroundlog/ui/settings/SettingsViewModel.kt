package com.gexetr.lawnroundlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lawnroundlog.data.SettingsStore
import com.gexetr.lawnroundlog.ui.AppearanceMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the settings surface draws, in one object.
 *
 * One object rather than four flows because the surface is a single column: four independent
 * collections would redraw it four times for one user action, and every row would need its own
 * placeholder.
 *
 * Every value here is a preference the rest of the app reads, which is what makes this a settings
 * model rather than a screen's scratch space: `appearance` reaches the theme host, `currencySymbol`
 * and `defaultVisitAmountUsd` reach the amount shown and pre-filled on a visit, `confirmBeforeDelete`
 * reaches the delete path, and `hapticsEnabled` reaches the controls that record a visit.
 */
data class SettingsUiState(
    val appearance: AppearanceMode = AppearanceMode.FOLLOW_SYSTEM,
    val currencySymbol: String = SettingsStore.DEFAULT_CURRENCY_SYMBOL,
    val defaultVisitAmountUsd: Double = SettingsStore.DEFAULT_VISIT_AMOUNT_USD_VALUE,
    val confirmBeforeDelete: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

/** What a text field was told but could not accept, so the row can explain itself. */
enum class SettingsDraftProblem {
    /** The currency symbol is longer than [SettingsViewModel.MAX_CURRENCY_SYMBOL_LENGTH]. */
    CURRENCY_TOO_LONG,

    /** The default amount is not a number, or is negative. */
    AMOUNT_NOT_A_NUMBER,
}

/**
 * The only thing between this surface and the two places a preference lives.
 *
 * The behaviour preferences are persisted through [SettingsStore], which owns the DataStore: nothing
 * under `ui/` names a `DataStore` type and nothing declares a second `preferencesDataStore`, so where
 * a preference is written stays one layer down. The screen calls [setConfirmBeforeDelete] and the
 * click is over — it never touches a file.
 *
 * Appearance follows the same DataStore path as the other preferences: the theme changes immediately
 * and the stored choice restores when the process returns, so Settings never offers a temporary mode.
 */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val draftProblem = kotlinx.coroutines.flow.MutableStateFlow<SettingsDraftProblem?>(null)

    private data class StoredPreferences(
        val currencySymbol: String,
        val defaultVisitAmountUsd: Double,
        val confirmBeforeDelete: Boolean,
        val hapticsEnabled: Boolean,
    )

    /**
     * The four stored preferences as one stream.
     *
     * The initial value exists only so the surface can draw before DataStore's first read lands, which
     * is a few milliseconds; the real value replaces it on the first emission. The two literals below
     * are the same defaults `SettingsStore` keeps in its own private companion — restated because that
     * companion is private, and read as a one-frame display fallback rather than as a second source of
     * truth for a stored value.
     */
    private val stored: StateFlow<StoredPreferences> = combine(
        store.currencySymbol,
        store.defaultVisitAmountUsd,
        store.confirmBeforeDelete,
        store.hapticsEnabled,
    ) { currency, amount, confirm, haptics ->
        StoredPreferences(currency, amount, confirm, haptics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StoredPreferences(
            currencySymbol = SettingsStore.DEFAULT_CURRENCY_SYMBOL,
            defaultVisitAmountUsd = SettingsStore.DEFAULT_VISIT_AMOUNT_USD_VALUE,
            confirmBeforeDelete = true,
            hapticsEnabled = true,
        ),
    )

    val state: StateFlow<SettingsUiState> = combine(
        stored,
        store.appearance,
    ) { preferences, mode ->
        SettingsUiState(
            appearance = mode,
            currencySymbol = preferences.currencySymbol,
            defaultVisitAmountUsd = preferences.defaultVisitAmountUsd,
            confirmBeforeDelete = preferences.confirmBeforeDelete,
            hapticsEnabled = preferences.hapticsEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /** The last rejected edit, so a row can say what was wrong instead of silently ignoring a tap. */
    val problem: StateFlow<SettingsDraftProblem?> = draftProblem

    fun setAppearance(mode: AppearanceMode) {
        viewModelScope.launch { store.setAppearance(mode) }
    }

    /**
     * A currency symbol is one to three characters. A longer value is a slip, and storing it would put
     * the operator's typo in front of every amount in the app, so it is reported and not written.
     */
    fun setCurrencySymbol(value: String) {
        val trimmed = value.trim()
        if (trimmed.length > MAX_CURRENCY_SYMBOL_LENGTH) {
            draftProblem.value = SettingsDraftProblem.CURRENCY_TOO_LONG
            return
        }
        draftProblem.value = null
        viewModelScope.launch { store.setCurrencySymbol(trimmed) }
    }

    /**
     * A comma is accepted as the decimal separator: a keyboard set to a locale that writes `12,50` is
     * an ordinary way to fill this field, and refusing it would look like a broken field.
     */
    fun setDefaultVisitAmount(value: String) {
        val normalized = value.trim().replace(',', '.')
        val amount = if (normalized.isEmpty()) 0.0 else normalized.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            draftProblem.value = SettingsDraftProblem.AMOUNT_NOT_A_NUMBER
            return
        }
        draftProblem.value = null
        viewModelScope.launch { store.setDefaultVisitAmountUsd(amount) }
    }

    fun setConfirmBeforeDelete(value: Boolean) {
        viewModelScope.launch { store.setConfirmBeforeDelete(value) }
    }

    fun setHapticsEnabled(value: Boolean) {
        viewModelScope.launch { store.setHapticsEnabled(value) }
    }

    companion object {
        /** One to three characters: a glyph, or an ISO code such as `USD`. */
        const val MAX_CURRENCY_SYMBOL_LENGTH: Int = 3

        fun factory(store: SettingsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(store) as T
            }
    }
}

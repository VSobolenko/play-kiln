package com.gexetr.plinthproductphotos.ui.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.data.AppSettings
import com.gexetr.plinthproductphotos.data.DataStoreSettingsStore
import com.gexetr.plinthproductphotos.data.SettingsStore
import com.gexetr.plinthproductphotos.data.ThemeMode
import com.gexetr.plinthproductphotos.data.StudioDefaults
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The settings screen's state holder.
 *
 * It owns no state of its own: the DataStore is the single source of truth, so the ViewModel forwards
 * the store's flow and the user's writes. A `MutableStateFlow` mirror here would be a second copy of
 * the truth that can drift from the file — the defect a settings screen is most likely to ship,
 * because a mirror looks correct until two writers disagree.
 *
 * `WhileSubscribed(5_000)` keeps the flow alive across a configuration change without keeping it
 * alive while the screen is off the back stack.
 */
class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    private val _saveError = MutableStateFlow(false)
    val saveError: StateFlow<Boolean> = _saveError.asStateFlow()

    val state: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setHapticsEnabled(enabled) }
    }

    fun updateStudioDefaults(transform: (StudioDefaults) -> StudioDefaults) {
        viewModelScope.launch {
            try {
                store.updateStudioDefaults(transform)
                _saveError.value = false
            } catch (failure: IOException) {
                Log.e("PlinthSettings", "Could not save studio defaults", failure)
                _saveError.value = true
            }
        }
    }

    companion object {
        /**
         * Builds the ViewModel with the app's real store.
         *
         * The application context is taken here rather than remembered in the composition: a DataStore
         * instance is per file, not per composition, and holding an Activity context in it would be a
         * leak. It also keeps `DataStore` out of the screen, so the screen has nothing to reach past.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(DataStoreSettingsStore(applicationContext)) as T
            }
        }
    }
}

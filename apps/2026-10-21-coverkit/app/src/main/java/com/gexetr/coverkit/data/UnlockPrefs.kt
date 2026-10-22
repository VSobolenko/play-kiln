package com.gexetr.coverkit.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Local unlock flag. Production entitlement comes from Play Billing later; this DataStore key
 * ([UNLOCK_OWNED_KEY]) is the debug override and the test seam.
 */
interface UnlockPrefs {
    val unlockOwned: Flow<Boolean>
    suspend fun setUnlockOwned(owned: Boolean)
}

private val Context.unlockPrefsDataStore by preferencesDataStore(name = "unlock_prefs")

class DataStoreUnlockPrefs(
    private val context: Context,
) : UnlockPrefs {

    override val unlockOwned: Flow<Boolean> = context.unlockPrefsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[UNLOCK_OWNED] ?: false }

    override suspend fun setUnlockOwned(owned: Boolean) {
        context.unlockPrefsDataStore.edit { preferences ->
            preferences[UNLOCK_OWNED] = owned
        }
    }

    companion object {
        /** Exposed for tests and billing wiring that must share one key name. */
        const val UNLOCK_OWNED_KEY: String = "unlockOwned"

        val UNLOCK_OWNED = booleanPreferencesKey(UNLOCK_OWNED_KEY)
    }
}

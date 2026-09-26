package com.gexetr.pipesizing.ui.cases

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.savedCasesStore by preferencesDataStore(name = "saved_cases")

/**
 * The user's saved cases, newest first.
 *
 * The list is the only source of truth, so a load cannot show a case the store no longer has and a
 * delete cannot leave a ghost entry.
 */
interface CasesRepository {
    val cases: Flow<List<SavedCase>>

    suspend fun upsert(case: SavedCase)

    suspend fun delete(id: String)

    suspend fun get(id: String): SavedCase?

    suspend fun clear()

    /** In-memory implementation for previews and tests; never a production backend. */
    class InMemory(initial: List<SavedCase> = emptyList()) : CasesRepository {
        private val state = MutableStateFlow(initial.sortedByDescending { it.savedAtEpochMillis })

        override val cases: Flow<List<SavedCase>> = state

        override suspend fun upsert(case: SavedCase) {
            state.value = (state.value.filterNot { it.id == case.id } + case)
                .sortedByDescending { it.savedAtEpochMillis }
        }

        override suspend fun delete(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun get(id: String): SavedCase? = state.value.firstOrNull { it.id == id }

        override suspend fun clear() {
            state.value = emptyList()
        }
    }
}

class DataStoreCasesRepository(private val context: Context) : CasesRepository {

    override val cases: Flow<List<SavedCase>> =
        context.savedCasesStore.data.map { preferences ->
            SavedCaseCodec.decode(preferences[KEY_CASES].orEmpty())
                .sortedByDescending { it.savedAtEpochMillis }
        }

    override suspend fun upsert(case: SavedCase) {
        context.savedCasesStore.edit { preferences ->
            val current = SavedCaseCodec.decode(preferences[KEY_CASES].orEmpty())
            preferences[KEY_CASES] = SavedCaseCodec.encode(
                (current.filterNot { it.id == case.id } + case).sortedByDescending { it.savedAtEpochMillis },
            )
        }
    }

    override suspend fun delete(id: String) {
        context.savedCasesStore.edit { preferences ->
            val current = SavedCaseCodec.decode(preferences[KEY_CASES].orEmpty())
            preferences[KEY_CASES] = SavedCaseCodec.encode(current.filterNot { it.id == id })
        }
    }

    override suspend fun get(id: String): SavedCase? =
        cases.first().firstOrNull { it.id == id }

    override suspend fun clear() {
        context.savedCasesStore.edit { preferences -> preferences.remove(KEY_CASES) }
    }

    private companion object {
        val KEY_CASES = stringPreferencesKey("cases")
    }
}

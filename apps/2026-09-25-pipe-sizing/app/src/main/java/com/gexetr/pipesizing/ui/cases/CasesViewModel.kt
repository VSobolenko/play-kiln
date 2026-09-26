package com.gexetr.pipesizing.ui.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The free/paid boundary for the saved-case list, as a pure function.
 *
 * It lives outside the composable so the rule that the free tier's limit is exactly
 * `MonetizationConfig.FREE_CASE_LIMIT` cases and that Pro removes it is asserted in a JVM test
 * rather than being re-derived from whatever the UI happens to show.
 */
fun canAddCase(currentCount: Int, freeLimit: Int, pro: Boolean): Boolean =
    pro || currentCount < freeLimit

class CasesViewModel(private val repository: CasesRepository) : ViewModel() {

    val cases: StateFlow<List<SavedCase>> = repository.cases.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    suspend fun delete(id: String) = repository.delete(id)

    /** Stores the working case under [name]. The caller decides whether the limit allows it. */
    suspend fun save(name: String, fields: Map<String, String>): SavedCase {
        val saved = SavedCase.new(
            name = name.trim(),
            fields = fields,
            nowEpochMillis = System.currentTimeMillis(),
        )
        repository.upsert(saved)
        return saved
    }

    companion object {
        fun factory(repository: CasesRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CasesViewModel(repository) }
        }
    }
}

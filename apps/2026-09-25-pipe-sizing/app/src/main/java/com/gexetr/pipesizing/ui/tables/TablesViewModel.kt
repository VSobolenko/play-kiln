package com.gexetr.pipesizing.ui.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the table surface can ask for. Editing the header and a row are separate events. */
sealed interface TablesEvent {
    data object StartNew : TablesEvent
    data class Open(val tableId: String) : TablesEvent
    data object DiscardEditing : TablesEvent
    data class HeaderChanged(val field: TableHeaderField, val value: String) : TablesEvent
    data object AddRow : TablesEvent
    data class RowChanged(val index: Int, val field: TableRowField, val value: String) : TablesEvent
    data class RemoveRow(val index: Int) : TablesEvent
    data object SaveEditing : TablesEvent
    data class DeleteTable(val tableId: String) : TablesEvent
}

/**
 * The state holder for the user's recorded tables.
 *
 * Mirrors `CasesViewModel`'s shape — the repository's flow is the list and the screen holds no state
 * of its own — and mirrors `HomeViewModel`'s testability trick of an explicit scope, because
 * `viewModelScope` needs a main dispatcher a plain JVM test does not have.
 *
 * `uiState` is a `stateIn` of the repository flow combined with the draft open for editing, so a
 * table deleted behind the screen's back cannot leave a ghost entry and the draft cannot outlive its
 * table.
 */
class TablesViewModel(
    private val repository: TablesRepository,
    scope: CoroutineScope? = null,
) : ViewModel() {

    private val workScope: CoroutineScope by lazy { scope ?: viewModelScope }

    private val editing = MutableStateFlow<TableDraft?>(null)

    val uiState: StateFlow<TablesUiState> = combine(repository.tables, editing) { tables, draft ->
        TablesUiState(tables = tables, editing = draft)
    }.stateIn(
        scope = workScope,
        // Eagerly, so `uiState.value` is the repository's current state rather than the empty
        // initial value until someone subscribes. The list is small and always on screen.
        started = SharingStarted.Eagerly,
        initialValue = TablesUiState(),
    )

    fun onEvent(event: TablesEvent) {
        when (event) {
            TablesEvent.StartNew -> editing.value = TableDraft.new()

            // The list is already in memory, so opening a table is synchronous and typing cannot race
            // the load. The repository read is only a fallback for an id the list does not carry.
            is TablesEvent.Open -> {
                val known = uiState.value.tables.firstOrNull { it.id == event.tableId }
                if (known != null) {
                    editing.value = TableDraft.from(known)
                } else {
                    workScope.launch { editing.value = repository.get(event.tableId)?.let(TableDraft::from) }
                }
            }

            TablesEvent.DiscardEditing -> editing.value = null

            is TablesEvent.HeaderChanged -> editing.update { draft -> draft?.withHeader(event.field, event.value) }

            TablesEvent.AddRow -> editing.update { draft -> draft?.plusRow() }

            is TablesEvent.RowChanged -> editing.update { draft -> draft?.withRow(event.index, event.field, event.value) }

            is TablesEvent.RemoveRow -> editing.update { draft -> draft?.minusRow(event.index) }

            // An unusable draft is never written: `toTableOrNull` runs the domain's own completeness
            // rule, so an incomplete header cannot reach the store by any route.
            TablesEvent.SaveEditing -> {
                val table = editing.value?.toTableOrNull() ?: return
                workScope.launch {
                    repository.upsert(table)
                    editing.value = null
                }
            }

            is TablesEvent.DeleteTable -> workScope.launch { repository.delete(event.tableId) }
        }
    }

    companion object {
        fun factory(repository: TablesRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TablesViewModel(repository) }
        }
    }
}

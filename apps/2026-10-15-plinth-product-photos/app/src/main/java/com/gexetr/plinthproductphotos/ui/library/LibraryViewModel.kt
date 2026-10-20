package com.gexetr.plinthproductphotos.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.data.repository.LibraryRepository
import com.gexetr.plinthproductphotos.domain.library.ItemOperationResult
import com.gexetr.plinthproductphotos.domain.library.librarySearchQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

data class LibraryUiState(
    val searchQuery: String = "",
    val category: String = "",
    val marketplaceId: String = "",
    val marketplaceTarget: String = "",
    val updatedAfter: String = "",
    val updatedBefore: String = "",
    val selectedProjectIds: Set<String> = emptySet(),
    val categories: List<String> = emptyList(),
    val marketplaces: List<String> = emptyList(),
    val operationResults: List<ItemOperationResult> = emptyList(),
    val message: String? = null,
    val filterError: String? = null,
)

private data class LibraryQuery(
    val searchQuery: String,
    val category: String,
    val marketplaceId: String,
    val updatedAfter: String,
    val updatedBefore: String,
    val filterError: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: LibraryRepository,
    private val exportOne: suspend (String) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val projects: StateFlow<PagingData<ProjectSummary>> = _state
        .map { state ->
            LibraryQuery(
                searchQuery = state.searchQuery,
                category = state.category,
                marketplaceId = state.marketplaceId,
                updatedAfter = state.updatedAfter,
                updatedBefore = state.updatedBefore,
                filterError = state.filterError,
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { filters ->
            if (filters.filterError != null) {
                flowOf(PagingData.empty<ProjectSummary>())
            } else {
                repository.library(
                    search = librarySearchQuery(filters.searchQuery),
                    category = filters.category.takeIf(String::isNotBlank),
                    marketplaceId = filters.marketplaceId.takeIf(String::isNotBlank),
                    updatedAfter = filters.updatedAfter.toEpoch(endOfDay = false),
                    updatedBefore = filters.updatedBefore.toEpoch(endOfDay = true),
                )
            }
        }
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PagingData.empty())

    init {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(categories = repository.categories(), marketplaces = repository.marketplaces())
                }
            } catch (failure: Exception) {
                _state.update { it.copy(message = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun search(value: String) = _state.update { it.copy(searchQuery = value) }

    fun setCategory(value: String) = _state.update { it.copy(category = value) }

    fun setMarketplace(value: String) = _state.update { it.copy(marketplaceId = value) }

    fun setMarketplaceTarget(value: String) = _state.update { it.copy(marketplaceTarget = value) }

    fun setUpdatedAfter(value: String) = updateDate { it.copy(updatedAfter = value) }

    fun setUpdatedBefore(value: String) = updateDate { it.copy(updatedBefore = value) }

    fun clearFilters() = _state.update {
        it.copy(
            searchQuery = "",
            category = "",
            marketplaceId = "",
            updatedAfter = "",
            updatedBefore = "",
            filterError = null,
        )
    }

    fun toggleProject(projectId: String) = _state.update {
        it.copy(selectedProjectIds = if (projectId in it.selectedProjectIds) {
            it.selectedProjectIds - projectId
        } else {
            it.selectedProjectIds + projectId
        })
    }

    fun toggleProjects(projectIds: List<String>) = _state.update {
        val selected = if (projectIds.all(it.selectedProjectIds::contains)) {
            it.selectedProjectIds - projectIds.toSet()
        } else {
            it.selectedProjectIds + projectIds
        }
        it.copy(selectedProjectIds = selected)
    }

    fun clearSelection() = _state.update { it.copy(selectedProjectIds = emptySet()) }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(projectId)
                _state.update { it.copy(selectedProjectIds = it.selectedProjectIds - projectId, message = null) }
            } catch (failure: Exception) {
                _state.update { it.copy(message = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun deleteSelectedProjects() {
        val projectIds = _state.value.selectedProjectIds.toList()
        viewModelScope.launch {
            val failures = mutableListOf<String>()
            projectIds.forEach { id ->
                try {
                    repository.deleteProject(id)
                } catch (failure: Exception) {
                    failures += "$id: ${failure.message ?: failure::class.simpleName}"
                }
            }
            _state.update {
                it.copy(
                    selectedProjectIds = emptySet(),
                    message = failures.takeIf { it.isNotEmpty() }?.joinToString(),
                )
            }
        }
    }

    fun changeMarketplaceForSelected(marketplaceId: String) {
        val target = _state.value.marketplaceTarget
        if (target.isBlank()) {
            _state.update { it.copy(message = "marketplace-required") }
            return
        }
        val selected = _state.value.selectedProjectIds.toList()
        viewModelScope.launch {
            try {
                val itemIds = selected.flatMap { projectId -> repository.itemsForProject(projectId).map { it.id } }
                val results = repository.changeMarketplace(itemIds, target)
                _state.update { it.copy(operationResults = results, selectedProjectIds = emptySet(), message = null) }
            } catch (failure: Exception) {
                _state.update { it.copy(message = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun reExportSelected() {
        val selected = _state.value.selectedProjectIds.toList()
        viewModelScope.launch {
            try {
                val itemIds = selected.flatMap { projectId -> repository.itemsForProject(projectId).map { it.id } }
                val results = repository.reExport(itemIds) { item -> exportOne(item.id) }
                _state.update { it.copy(operationResults = results, selectedProjectIds = emptySet(), message = null) }
            } catch (failure: Exception) {
                _state.update { it.copy(message = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun dismissResults() = _state.update { it.copy(operationResults = emptyList(), message = null) }

    private fun updateDate(transform: (LibraryUiState) -> LibraryUiState) {
        _state.update { current ->
            val updated = transform(current)
            val valid = updated.updatedAfter.isValidDate()
            val validEnd = updated.updatedBefore.isValidDate()
            updated.copy(filterError = if (valid && validEnd) null else "invalid-date")
        }
    }

    private fun String.isValidDate(): Boolean = isBlank() || try {
        LocalDate.parse(this)
        true
    } catch (_: java.time.format.DateTimeParseException) {
        false
    }

    private fun String.toEpoch(endOfDay: Boolean): Long? {
        if (isBlank()) return null
        val date = LocalDate.parse(this)
        val instant = if (endOfDay) date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1)
        else date.atStartOfDay(ZoneOffset.UTC).toInstant()
        return instant.toEpochMilli()
    }
}

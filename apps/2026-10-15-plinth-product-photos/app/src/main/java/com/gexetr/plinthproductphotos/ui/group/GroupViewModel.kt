package com.gexetr.plinthproductphotos.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.data.db.GroupEntity
import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.repository.LibraryRepository
import com.gexetr.plinthproductphotos.domain.group.GroupPhoto
import com.gexetr.plinthproductphotos.domain.group.Grouping
import com.gexetr.plinthproductphotos.domain.group.SkuPattern
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupUiState(
    val pattern: String = "{base}-{n}",
    val skuBase: String = "item",
    val photos: List<ItemEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val error: String? = null,
)

class GroupViewModel(
    private val projectId: String,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupUiState())
    val state: StateFlow<GroupUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun setPattern(value: String) = _state.update { it.copy(pattern = value) }

    fun setSkuBase(value: String) = _state.update { it.copy(skuBase = value) }

    fun regroup() {
        viewModelScope.launch {
            try {
                val pattern = SkuPattern(_state.value.pattern)
                val photos = repository.itemsForProject(projectId).sortedWith(compareBy(ItemEntity::photoOrder, ItemEntity::id))
                val validSkus = photos.mapNotNull { it.sku?.takeIf { sku -> pattern.parse(sku) != null } }.toSet()
                val unassigned = photos.filter { it.sku == null || pattern.parse(it.sku) == null }
                val unassignedIds = unassigned.map(ItemEntity::id)
                val assigned = unassignedIds.zip(
                    pattern.assignUnique(_state.value.skuBase, unassignedIds, validSkus),
                ).toMap()
                val groups = Grouping.regroup(
                    photos.map { photo ->
                        GroupPhoto(
                            id = photo.id,
                            sku = photo.sku?.takeIf { pattern.parse(it) != null } ?: requireNotNull(assigned[photo.id]),
                            photoOrder = photo.photoOrder,
                            role = photo.role,
                        )
                    },
                    pattern,
                )
                repository.replaceProjectGrouping(projectId, pattern.template, groups)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun createGroup() {
        viewModelScope.launch {
            try {
                repository.createGroup(
                    projectId = projectId,
                    skuBase = _state.value.skuBase,
                    skuPattern = SkuPattern(_state.value.pattern).template,
                    ordinal = (_state.value.groups.maxOfOrNull(GroupEntity::ordinal) ?: -1) + 1,
                )
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun renameGroup(groupId: String, skuBase: String) {
        viewModelScope.launch {
            try {
                repository.renameGroup(groupId, skuBase, _state.value.pattern)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun moveItem(itemId: String, groupId: String) {
        viewModelScope.launch {
            try {
                val targetOrder = _state.value.photos.count { it.groupId == groupId }
                repository.moveItem(itemId, groupId, targetOrder)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun setReference(itemId: String, reference: Boolean) {
        viewModelScope.launch {
            try {
                repository.setPhotoRole(itemId, if (reference) SlotRole.REFERENCE else SlotRole.MAIN)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun saveCategory(itemId: String, category: String) {
        viewModelScope.launch {
            try {
                repository.setCategory(itemId, category.takeIf(String::isNotBlank))
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun confirmSku(itemId: String) {
        viewModelScope.launch {
            try {
                repository.confirmSuggestedSku(itemId)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun confirmScale(itemId: String) {
        viewModelScope.launch {
            try {
                repository.confirmScaleHint(itemId)
                reload()
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun reload() {
        viewModelScope.launch {
            try {
                val photos = repository.itemsForProject(projectId)
                val groups = repository.groupsForProject(projectId).map { it.group }
                _state.update { it.copy(photos = photos, groups = groups, error = null) }
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: failure::class.simpleName) }
            }
        }
    }
}

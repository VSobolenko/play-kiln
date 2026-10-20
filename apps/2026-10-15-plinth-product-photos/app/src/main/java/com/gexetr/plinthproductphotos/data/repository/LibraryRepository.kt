package com.gexetr.plinthproductphotos.data.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.gexetr.plinthproductphotos.data.db.GroupEntity
import com.gexetr.plinthproductphotos.data.db.GroupWithItems
import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.db.PlinthDatabase
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.RecentPhotoStore
import com.gexetr.plinthproductphotos.data.project.deleteProjectFromIndexes
import com.gexetr.plinthproductphotos.domain.group.PhotoGroup
import com.gexetr.plinthproductphotos.domain.group.SkuPattern
import com.gexetr.plinthproductphotos.domain.library.ItemOperationResult
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class LibraryRepository(context: Context, private val database: PlinthDatabase = PlinthDatabase.get(context)) {
    private val appContext = context.applicationContext

    suspend fun importPhoto(photo: ProjectPhoto, saveRecentPhoto: () -> Unit) {
        val source = ownedFile(photo.sourcePath)
        require(source.isFile) { "Imported source photo is missing" }
        database.withTransaction {
            database.projectDao().importPhoto(photo.copy(sourcePath = source.path))
            saveRecentPhoto()
        }
    }

    fun library(
        search: String? = null,
        category: String? = null,
        marketplaceId: String? = null,
        updatedAfter: Long? = null,
        updatedBefore: Long? = null,
    ): Flow<PagingData<ProjectSummary>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = {
            database.projectDao().pageLibrary(search, category, marketplaceId, updatedAfter, updatedBefore)
        },
    ).flow

    suspend fun categories(): List<String> = database.projectDao().categories()

    suspend fun marketplaces(): List<String> = database.projectDao().marketplaces()

    fun searchBySku(sku: String): PagingSource<Int, ItemEntity> = database.itemDao().bySku(sku)

    fun searchByCategory(category: String): PagingSource<Int, ItemEntity> = database.itemDao().byCategory(category)

    fun searchByMarketplace(marketplaceId: String): PagingSource<Int, ItemEntity> =
        database.itemDao().byMarketplace(marketplaceId)

    fun searchText(query: String): PagingSource<Int, ItemEntity> = database.itemDao().searchText(query)

    suspend fun itemsForProject(projectId: String): List<ItemEntity> = database.itemDao().byProject(projectId)

    suspend fun groupsForProject(projectId: String): List<GroupWithItems> = database.groupDao().withItems(projectId)

    suspend fun changeMarketplace(
        selectedItemIds: List<String>,
        marketplaceId: String,
    ): List<ItemOperationResult> {
        require(selectedItemIds.distinct().size == selectedItemIds.size) { "Selected item ids must be unique" }
        val results = mutableListOf<ItemOperationResult>()
        for (id in selectedItemIds) {
            try {
                database.withTransaction {
                    check(database.itemDao().setMarketplace(id, marketplaceId) == 1) {
                        "Item '$id' no longer exists"
                    }
                }
                results += ItemOperationResult(id, succeeded = true)
            } catch (failure: Exception) {
                results += ItemOperationResult(
                    itemId = id,
                    succeeded = false,
                    error = failure.message ?: failure::class.simpleName ?: "Operation failed",
                )
            }
        }
        return results
    }

    suspend fun reExport(
        selectedItemIds: List<String>,
        exportOne: suspend (ItemEntity) -> Unit,
    ): List<ItemOperationResult> {
        require(selectedItemIds.distinct().size == selectedItemIds.size) { "Selected item ids must be unique" }
        return selectedItemIds.map { id ->
            try {
                val item = requireNotNull(database.itemDao().byId(id)) { "Item '$id' no longer exists" }
                require(item.role != SlotRole.REFERENCE) { "Reference photos cannot be exported" }
                exportOne(item)
                ItemOperationResult(id, succeeded = true)
            } catch (failure: Exception) {
                ItemOperationResult(
                    itemId = id,
                    succeeded = false,
                    error = failure.message ?: failure::class.simpleName ?: "Re-export failed",
                )
            }
        }
    }

    suspend fun deleteProject(projectId: String) {
        val project = requireNotNull(database.projectDao().byId(projectId)) { "Project '$projectId' does not exist" }
        val items = database.itemDao().byProject(projectId)
        val outputs = items.flatMap { database.outputDao().byItem(it.id) }
        val ownedFiles = buildList {
            project.previewPath?.let(::add)
            items.forEach { item ->
                add(item.sourcePath)
                item.maskPath?.let(::add)
            }
            outputs.forEach { add(it.path) }
        }.map(::ownedFile)
        ownedFiles.distinct().forEach { file ->
            if (file.exists() && !file.delete()) {
                throw IOException("Could not delete project file: ${file.name}")
            }
        }
        database.withTransaction {
            deleteProjectFromIndexes(projectId, RecentPhotoStore(appContext)) {
                database.projectDao().deleteById(projectId)
            }
        }
    }

    suspend fun createGroup(projectId: String, skuBase: String, skuPattern: String, ordinal: Int): GroupEntity {
        requireNotNull(database.projectDao().byId(projectId)) { "Project '$projectId' does not exist" }
        val group = GroupEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            skuBase = skuBase,
            skuPattern = skuPattern,
            ordinal = ordinal,
            createdAt = System.currentTimeMillis(),
        )
        database.groupDao().insert(group)
        return group
    }

    suspend fun moveItem(itemId: String, groupId: String?, photoOrder: Int) {
        database.withTransaction {
            val item = requireNotNull(database.itemDao().byId(itemId)) { "Item '$itemId' no longer exists" }
            if (groupId != null) {
                val group = requireNotNull(database.groupDao().byId(groupId)) { "Group '$groupId' no longer exists" }
                require(group.projectId == item.projectId) { "Items cannot be moved across projects" }
                val sku = SkuPattern(group.skuPattern).format(group.skuBase, group.ordinal)
                check(database.itemDao().setSku(itemId, sku) == 1) { "Item '$itemId' no longer exists" }
            }
            check(database.itemDao().moveToGroup(itemId, groupId, photoOrder) == 1) {
                "Item '$itemId' no longer exists"
            }
        }
    }

    suspend fun setSku(itemId: String, sku: String) {
        check(database.itemDao().setSku(itemId, sku) == 1) { "Item '$itemId' no longer exists" }
    }

    suspend fun setCategory(itemId: String, category: String?) {
        check(database.itemDao().setCategory(itemId, category) == 1) { "Item '$itemId' no longer exists" }
    }

    suspend fun replaceProjectGrouping(projectId: String, pattern: String, groups: List<PhotoGroup>) {
        database.withTransaction {
            requireNotNull(database.projectDao().byId(projectId)) { "Project '$projectId' does not exist" }
            database.groupDao().deleteForProject(projectId)
            val skuPattern = SkuPattern(pattern)
            groups.forEach { photoGroup ->
                val parsedSku = requireNotNull(skuPattern.parse(photoGroup.sku)) {
                    "Group SKU '${photoGroup.sku}' does not match its pattern"
                }
                val group = GroupEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    skuBase = parsedSku.base,
                    skuPattern = pattern,
                    ordinal = parsedSku.number,
                    createdAt = System.currentTimeMillis(),
                )
                database.groupDao().insert(group)
                photoGroup.photos.forEach { photo ->
                    check(database.itemDao().setSku(photo.id, photo.sku) == 1) {
                        "Item '${photo.id}' no longer exists"
                    }
                    check(database.itemDao().moveToGroup(photo.id, group.id, photo.photoOrder) == 1) {
                        "Item '${photo.id}' no longer exists"
                    }
                }
            }
        }
    }

    suspend fun renameGroup(groupId: String, skuBase: String, patternText: String) {
        val pattern = SkuPattern(patternText)
        database.withTransaction {
            val group = requireNotNull(database.groupDao().byId(groupId)) { "Group '$groupId' does not exist" }
            val groups = database.groupDao().withItems(group.projectId)
            val selected = groups.singleOrNull { it.group.id == groupId }
                ?: throw IllegalStateException("Group '$groupId' has no unique record")
            val otherSkus = groups.asSequence()
                .filterNot { it.group.id == groupId }
                .map { candidate ->
                    candidate.items.firstOrNull()?.sku
                        ?: SkuPattern(candidate.group.skuPattern).format(
                            candidate.group.skuBase,
                            candidate.group.ordinal,
                        )
                }
                .map(String::lowercase)
                .toSet()
            var ordinal = group.ordinal
            if (ordinal < 1) ordinal = 1
            var assignedSku = pattern.format(skuBase, ordinal)
            while (assignedSku.lowercase() in otherSkus) {
                check(ordinal < Int.MAX_VALUE) { "Could not assign a unique SKU" }
                ordinal++
                assignedSku = pattern.format(skuBase, ordinal)
            }
            database.groupDao().update(group.copy(skuBase = skuBase, skuPattern = patternText, ordinal = ordinal))
            selected.items.forEach { item ->
                check(database.itemDao().setSku(item.id, assignedSku) == 1) {
                    "Item '${item.id}' no longer exists"
                }
            }
        }
    }

    suspend fun setPhotoRole(itemId: String, role: SlotRole) {
        check(database.itemDao().setRole(itemId, role) == 1) { "Item '$itemId' no longer exists" }
    }

    suspend fun suggestSku(itemId: String, sku: String) {
        check(database.itemDao().suggestSku(itemId, sku) == 1) { "Item '$itemId' no longer exists" }
    }

    suspend fun confirmSuggestedSku(itemId: String) {
        check(database.itemDao().confirmSuggestedSku(itemId) == 1) { "No SKU suggestion is awaiting confirmation" }
    }

    suspend fun suggestScaleHint(itemId: String, hintJson: String) {
        check(database.itemDao().suggestScaleHint(itemId, hintJson) == 1) { "Item '$itemId' no longer exists" }
    }

    suspend fun confirmScaleHint(itemId: String) {
        check(database.itemDao().confirmScaleHint(itemId) == 1) { "No scale suggestion is awaiting confirmation" }
    }

    private fun ownedFile(path: String): File {
        val filesRoot = appContext.filesDir.canonicalFile
        val candidate = File(path).let { if (it.isAbsolute) it else File(filesRoot, path) }.canonicalFile
        require(candidate.toPath().startsWith(filesRoot.toPath())) { "Resource path is outside app storage" }
        return candidate
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

package com.gexetr.plinthproductphotos.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.JobState
import com.gexetr.plinthproductphotos.domain.model.SlotRole

data class ProjectSummary(
    @Embedded val project: ProjectEntity,
    val itemCount: Int,
    val primarySku: String?,
)

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImportedProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImportedItem(item: ItemEntity)

    @Query("SELECT * FROM item WHERE id = :itemId")
    suspend fun importedItem(itemId: String): ItemEntity?

    @Transaction
    suspend fun importPhoto(photo: ProjectPhoto) {
        if (importedItem(photo.id) != null) return
        insertImportedProject(
            ProjectEntity(
                id = photo.id,
                name = photo.name,
                createdAt = photo.importedAt,
                updatedAt = photo.importedAt,
                previewPath = photo.sourcePath,
                category = null,
                marketplaceId = null,
                tags = null,
            ),
        )
        insertImportedItem(
            ItemEntity(
                id = photo.id,
                projectId = photo.id,
                sourcePath = photo.sourcePath,
                sku = null,
                groupId = null,
                role = SlotRole.MAIN,
                category = null,
                categoryConfirmed = false,
                attributesJson = null,
                scaleHintJson = null,
                scaleHintConfirmed = false,
                suggestedSku = null,
                skuConfirmed = false,
                photoOrder = 0,
                marketplaceId = null,
                maskPath = null,
                maskRevision = 0,
                state = ItemState.IMPORTED,
                lastError = null,
            ),
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT * FROM project WHERE id = :projectId")
    suspend fun byId(projectId: String): ProjectEntity?

    @Query(
        """
        SELECT project.*,
            (SELECT COUNT(*) FROM item WHERE item.projectId = project.id) AS itemCount,
            (SELECT sku FROM item WHERE item.projectId = project.id AND sku IS NOT NULL
                ORDER BY item.photoOrder, item.id LIMIT 1) AS primarySku
        FROM project
        WHERE (:search IS NULL OR :search = '' OR
            instr(lower(project.name), lower(:search)) > 0 OR
            instr(lower(COALESCE(project.tags, '')), lower(:search)) > 0 OR
            EXISTS (SELECT 1 FROM item WHERE item.projectId = project.id AND (
                instr(lower(COALESCE(item.sku, '')), lower(:search)) > 0 OR
                instr(lower(COALESCE(item.category, '')), lower(:search)) > 0
            )))
        AND (:category IS NULL OR project.category = :category OR
            EXISTS (SELECT 1 FROM item WHERE item.projectId = project.id AND item.category = :category))
        AND (:marketplaceId IS NULL OR project.marketplaceId = :marketplaceId OR
            EXISTS (SELECT 1 FROM item WHERE item.projectId = project.id AND item.marketplaceId = :marketplaceId))
        AND (:updatedAfter IS NULL OR project.updatedAt >= :updatedAfter)
        AND (:updatedBefore IS NULL OR project.updatedAt <= :updatedBefore)
        ORDER BY project.updatedAt DESC, project.id
        """,
    )
    fun pageLibrary(
        search: String?,
        category: String?,
        marketplaceId: String?,
        updatedAfter: Long?,
        updatedBefore: Long?,
    ): PagingSource<Int, ProjectSummary>

    @Query("SELECT DISTINCT category FROM item WHERE category IS NOT NULL ORDER BY category")
    suspend fun categories(): List<String>

    @Query("SELECT DISTINCT marketplaceId FROM item WHERE marketplaceId IS NOT NULL ORDER BY marketplaceId")
    suspend fun marketplaces(): List<String>

    @Query("SELECT * FROM project WHERE id IN (:projectIds)")
    suspend fun selectedProjects(projectIds: List<String>): List<ProjectEntity>

    @Query("DELETE FROM project WHERE id = :projectId")
    suspend fun deleteById(projectId: String): Int
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("SELECT * FROM item WHERE id = :itemId")
    suspend fun byId(itemId: String): ItemEntity?

    @Query("SELECT * FROM item WHERE projectId = :projectId ORDER BY photoOrder, id")
    suspend fun byProject(projectId: String): List<ItemEntity>

    @Query("SELECT * FROM item WHERE sku = :sku ORDER BY projectId, photoOrder, id")
    fun bySku(sku: String): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM item WHERE category = :category ORDER BY projectId, photoOrder, id")
    fun byCategory(category: String): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM item WHERE marketplaceId = :marketplaceId ORDER BY projectId, photoOrder, id")
    fun byMarketplace(marketplaceId: String): PagingSource<Int, ItemEntity>

    @Query(
        """
        SELECT * FROM item
        WHERE instr(lower(COALESCE(sku, '')), lower(:query)) > 0
           OR instr(lower(COALESCE(category, '')), lower(:query)) > 0
           OR instr(lower(sourcePath), lower(:query)) > 0
        ORDER BY projectId, photoOrder, id
        """,
    )
    fun searchText(query: String): PagingSource<Int, ItemEntity>

    @Query("SELECT * FROM item WHERE groupId = :groupId ORDER BY photoOrder, id")
    suspend fun byGroup(groupId: String): List<ItemEntity>

    @Query("SELECT * FROM item WHERE id IN (:itemIds) ORDER BY projectId, photoOrder, id")
    suspend fun selected(itemIds: List<String>): List<ItemEntity>

    @Query("UPDATE item SET marketplaceId = :marketplaceId WHERE id = :itemId")
    suspend fun setMarketplace(itemId: String, marketplaceId: String): Int

    @Query("UPDATE item SET groupId = :groupId, photoOrder = :photoOrder WHERE id = :itemId")
    suspend fun moveToGroup(itemId: String, groupId: String?, photoOrder: Int): Int

    @Query("UPDATE item SET sku = :sku, skuConfirmed = 1 WHERE id = :itemId")
    suspend fun setSku(itemId: String, sku: String): Int

    @Query("UPDATE item SET category = :category, categoryConfirmed = 1 WHERE id = :itemId")
    suspend fun setCategory(itemId: String, category: String?): Int

    @Query("UPDATE item SET role = :role WHERE id = :itemId")
    suspend fun setRole(itemId: String, role: SlotRole): Int

    @Query("UPDATE item SET suggestedSku = :sku, skuConfirmed = 0 WHERE id = :itemId")
    suspend fun suggestSku(itemId: String, sku: String): Int

    @Query("UPDATE item SET sku = suggestedSku, skuConfirmed = 1 WHERE id = :itemId AND suggestedSku IS NOT NULL")
    suspend fun confirmSuggestedSku(itemId: String): Int

    @Query("UPDATE item SET scaleHintJson = :hintJson, scaleHintConfirmed = 0 WHERE id = :itemId")
    suspend fun suggestScaleHint(itemId: String, hintJson: String): Int

    @Query("UPDATE item SET scaleHintConfirmed = 1 WHERE id = :itemId AND scaleHintJson IS NOT NULL")
    suspend fun confirmScaleHint(itemId: String): Int

}

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Update
    suspend fun update(group: GroupEntity)

    @Transaction
    @Query("SELECT * FROM `group` WHERE projectId = :projectId ORDER BY ordinal, id")
    suspend fun withItems(projectId: String): List<GroupWithItems>

    @Query("SELECT * FROM `group` WHERE id = :groupId")
    suspend fun byId(groupId: String): GroupEntity?

    @Query("DELETE FROM `group` WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String): Int
}

@Dao
interface LookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(look: LookEntity)

    @Query("SELECT * FROM look ORDER BY createdAt DESC, id")
    suspend fun all(): List<LookEntity>

    @Query("DELETE FROM look WHERE id = :lookId")
    suspend fun deleteById(lookId: String): Int
}

@Dao
interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Query("SELECT * FROM job WHERE id = :jobId")
    suspend fun byId(jobId: String): JobEntity?

    @Query("SELECT * FROM job WHERE state = :state AND nextAttemptAt <= :now ORDER BY createdAt, id")
    suspend fun due(state: JobState, now: Long): List<JobEntity>

    @Update
    suspend fun update(job: JobEntity)
}

@Dao
interface OutputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(output: OutputEntity)

    @Query("SELECT * FROM output WHERE itemId = :itemId ORDER BY createdAt DESC, id")
    suspend fun byItem(itemId: String): List<OutputEntity>

    @Query("SELECT * FROM output WHERE id = :outputId")
    suspend fun byId(outputId: String): OutputEntity?

    @Query(
        """
        SELECT output.* FROM output
        INNER JOIN item ON item.id = output.itemId
        WHERE item.projectId = :projectId
          AND item.role != 'REFERENCE'
          AND output.role != 'REFERENCE'
        ORDER BY item.photoOrder, output.createdAt, output.id
        """,
    )
    suspend fun exportableOutputs(projectId: String): List<OutputEntity>

    @Query("DELETE FROM output WHERE id = :outputId")
    suspend fun deleteById(outputId: String): Int
}

@Dao
interface ExportRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExportRecordEntity)

    @Query("SELECT * FROM export_record WHERE projectId = :projectId ORDER BY createdAt DESC, id")
    suspend fun byProject(projectId: String): List<ExportRecordEntity>
}

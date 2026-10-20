package com.gexetr.plinthproductphotos.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.model.JobState

@Entity(
    tableName = "project",
    indices = [Index(value = ["updatedAt"])],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val previewPath: String?,
    val category: String?,
    val marketplaceId: String?,
    val tags: String?,
)

@Entity(
    tableName = "group",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId", "skuBase"])],
)
data class GroupEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val skuBase: String,
    val skuPattern: String,
    val ordinal: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "item",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["projectId", "state", "sku"]),
        Index(value = ["groupId", "photoOrder"]),
        Index(value = ["projectId", "category"]),
        Index(value = ["projectId", "marketplaceId"]),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sourcePath: String,
    val sku: String?,
    val groupId: String?,
    val role: SlotRole,
    val category: String?,
    val categoryConfirmed: Boolean,
    val attributesJson: String?,
    val scaleHintJson: String?,
    val scaleHintConfirmed: Boolean,
    val suggestedSku: String?,
    val skuConfirmed: Boolean,
    val photoOrder: Int,
    val marketplaceId: String?,
    val maskPath: String?,
    val maskRevision: Int,
    val state: ItemState,
    val lastError: String?,
)

data class GroupWithItems(
    @Embedded val group: GroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val items: List<ItemEntity>,
)

@Entity(tableName = "look")
data class LookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val backgroundJson: String,
    val shadowJson: String,
    val marginPct: Float,
    val fillTarget: Float,
    val slideStyleId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "job",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["state", "nextAttemptAt", "projectId"])],
)
data class JobEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val itemId: String?,
    val state: JobState,
    val workName: String?,
    val attempt: Int,
    val nextAttemptAt: Long?,
    val terminalAt: Long?,
    val createdAt: Long,
    val lastError: String?,
)

@Entity(
    tableName = "output",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["itemId", "createdAt"])],
)
data class OutputEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val path: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val format: String,
    val provenance: Provenance,
    val operationsJson: String,
    val marketplaceId: String?,
    val role: SlotRole,
    val createdAt: Long,
)

@Entity(
    tableName = "export_record",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId", "createdAt"])],
)
data class ExportRecordEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val marketplaceId: String?,
    val manifestPath: String?,
    val createdAt: Long,
)

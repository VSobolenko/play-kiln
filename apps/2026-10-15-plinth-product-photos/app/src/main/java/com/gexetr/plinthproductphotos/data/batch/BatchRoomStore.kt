package com.gexetr.plinthproductphotos.data.batch

import androidx.room.withTransaction
import com.gexetr.plinthproductphotos.domain.batch.BatchItemState
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueItem
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BatchRoomStore(private val database: BatchDatabase) : BatchQueueStore {
    private val dao = database.batches()

    override suspend fun insertBatch(batchId: String, sourceIds: List<String>, lookJson: String) {
        require(sourceIds.isNotEmpty())
        require(sourceIds.none(String::isBlank))
        database.withTransaction {
            dao.insertBatch(
                BatchEntity(
                    batchId = batchId,
                    lookJson = lookJson,
                    workName = workName(batchId),
                    createdAt = System.currentTimeMillis(),
                ),
            )
            val storedLook = requireNotNull(dao.batch(batchId)) { "Batch row was not persisted" }.lookJson
            dao.insertItems(
                sourceIds.distinct().map { sourceId ->
                    BatchItemEntity(
                        id = itemId(batchId, sourceId),
                        batchId = batchId,
                        sourceId = sourceId,
                        lookJson = storedLook,
                        state = BatchItemState.QUEUED.name,
                    )
                },
            )
            dao.clearTerminal(batchId)
        }
    }

    override suspend fun items(batchId: String): List<BatchQueueItem> =
        dao.items(batchId).map(BatchItemEntity::toDomain)

    fun observeItems(batchId: String): Flow<List<BatchQueueItem>> =
        dao.observeItems(batchId).map { rows -> rows.map(BatchItemEntity::toDomain) }

    suspend fun batch(batchId: String): BatchEntity? = dao.batch(batchId)

    override suspend fun recoverInterrupted(batchId: String) {
        dao.recoverInterrupted(batchId)
    }

    override suspend fun claim(itemId: String): BatchQueueItem? {
        if (dao.claim(itemId) != 1) return null
        return requireNotNull(dao.item(itemId)) { "Claimed queue row disappeared" }.toDomain()
    }

    override suspend fun complete(itemId: String, outputPath: String) {
        dao.complete(itemId, outputPath)
        dao.item(itemId)?.let { dao.markTerminalIfSettled(it.batchId, System.currentTimeMillis()) }
    }

    override suspend fun fail(itemId: String, message: String) {
        dao.fail(itemId, message)
        dao.item(itemId)?.let { dao.markTerminalIfSettled(it.batchId, System.currentTimeMillis()) }
    }

    override suspend fun retry(itemId: String): String? = database.withTransaction {
        val item = dao.item(itemId) ?: return@withTransaction null
        if (dao.retry(itemId) != 1) return@withTransaction null
        dao.clearTerminal(item.batchId)
        item.batchId
    }

    suspend fun counts(batchId: String): BatchCounts = BatchCounts(
        completed = dao.composedCount(batchId),
        failed = dao.failedCount(batchId),
    )

    suspend fun settle(batchId: String) {
        dao.markTerminalIfSettled(batchId, System.currentTimeMillis())
    }

    companion object {
        fun workName(batchId: String): String = "plinth-batch:$batchId"
        fun itemId(batchId: String, sourceId: String): String = "$batchId:$sourceId"
    }
}

data class BatchCounts(val completed: Int, val failed: Int)

private fun BatchItemEntity.toDomain(): BatchQueueItem = BatchQueueItem(
    id = id,
    batchId = batchId,
    sourceId = sourceId,
    lookJson = lookJson,
    state = BatchItemState.valueOf(state),
    outputPath = outputPath,
    error = error,
    attempt = attempt,
)

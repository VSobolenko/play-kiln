package com.gexetr.plinthproductphotos.domain.batch

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class BatchItemState {
    QUEUED,
    PROCESSING,
    COMPOSED,
    FAILED,
    CANCELLED,
}

data class BatchQueueItem(
    val id: String,
    val batchId: String,
    val sourceId: String,
    val lookJson: String,
    val state: BatchItemState,
    val outputPath: String? = null,
    val error: String? = null,
    val attempt: Int = 0,
)

fun selectBatchSources(sourceIds: List<String>, selectedSourceIds: Set<String>): List<String> =
    sourceIds.distinct().filter(selectedSourceIds::contains)

interface BatchQueueStore {
    suspend fun insertBatch(batchId: String, sourceIds: List<String>, lookJson: String)
    suspend fun items(batchId: String): List<BatchQueueItem>
    suspend fun recoverInterrupted(batchId: String)
    suspend fun claim(itemId: String): BatchQueueItem?
    suspend fun complete(itemId: String, outputPath: String)
    suspend fun fail(itemId: String, message: String)
    suspend fun retry(itemId: String): String?
}

class BatchQueueProcessor(
    private val store: BatchQueueStore,
    private val compose: suspend (BatchQueueItem) -> String,
    private val maxConcurrent: Int = DEVICE_COMPOSITION_LIMIT,
) {
    init {
        require(maxConcurrent in 1..DEVICE_COMPOSITION_LIMIT) {
            "Device compositions may not exceed $DEVICE_COMPOSITION_LIMIT concurrent items"
        }
    }

    suspend fun applyLook(batchId: String, sourceIds: List<String>, lookJson: String) {
        require(batchId.isNotBlank())
        require(sourceIds.isNotEmpty())
        store.insertBatch(batchId, sourceIds.distinct(), lookJson)
        resume(batchId)
    }

    suspend fun resume(batchId: String) {
        store.recoverInterrupted(batchId)
        val queued = store.items(batchId).filter { it.state == BatchItemState.QUEUED }
        val semaphore = Semaphore(maxConcurrent)
        coroutineScope {
            queued.map { queuedItem ->
                async {
                    semaphore.withPermit {
                        val claimed = store.claim(queuedItem.id) ?: return@withPermit
                        val outputPath = try {
                            compose(claimed)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            store.fail(
                                claimed.id,
                                failure.message?.takeIf(String::isNotBlank) ?: failure.javaClass.simpleName,
                            )
                            return@withPermit
                        }
                        store.complete(claimed.id, outputPath)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun retry(itemId: String) {
        val batchId = store.retry(itemId) ?: return
        resume(batchId)
    }

    private companion object {
        const val DEVICE_COMPOSITION_LIMIT = 2
    }
}

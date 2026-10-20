package com.gexetr.plinthproductphotos.domain.batch

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchQueueProcessorTest {
    @Test
    fun batchSelectionExcludesDeselectedPhotosFromTheRun() = runBlocking {
        val sourceIds = listOf("photo-1", "photo-2", "photo-3")
        val selected = selectBatchSources(sourceIds, setOf("photo-1", "photo-3"))
        val store = FakeBatchQueueStore()

        BatchQueueProcessor(store, compose = { item -> "output-${item.id}" })
            .applyLook("batch", selected, "look-json")

        assertEquals(listOf("photo-1", "photo-3"), store.items.map { it.sourceId }.sorted())
    }

    @Test
    fun restartingSkipsItemsAlreadyFinished() = runBlocking {
        val store = FakeBatchQueueStore(
            items = listOf(
                BatchQueueItem("done", "batch", "photo-1", "look", BatchItemState.COMPOSED),
                BatchQueueItem("queued", "batch", "photo-2", "look", BatchItemState.QUEUED),
            ),
        )
        val processed = mutableListOf<String>()

        BatchQueueProcessor(store, compose = { item ->
            processed += item.id
            "output-${item.id}"
        }).resume("batch")

        assertEquals(listOf("queued"), processed)
        assertEquals(BatchItemState.COMPOSED, store.item("done").state)
        assertEquals(BatchItemState.COMPOSED, store.item("queued").state)
    }

    @Test
    fun oneItemFailureDoesNotStopOtherItems() = runBlocking {
        val store = FakeBatchQueueStore()
        val processor = BatchQueueProcessor(store, compose = { item ->
            if (item.id == "batch:photo-2") error("composition failed")
            "output-${item.id}"
        })
        processor.applyLook("batch", listOf("photo-1", "photo-2", "photo-3"), "look")

        assertEquals(BatchItemState.COMPOSED, store.item("batch:photo-1").state)
        assertEquals(BatchItemState.FAILED, store.item("batch:photo-2").state)
        assertEquals(BatchItemState.COMPOSED, store.item("batch:photo-3").state)
    }

    @Test
    fun retryProcessesOnlyTheFailedItem() = runBlocking {
        val store = FakeBatchQueueStore()
        val attempts = mutableMapOf<String, Int>()
        var failFirstAttempt = true
        val processor = BatchQueueProcessor(store, compose = { item ->
            attempts[item.id] = (attempts[item.id] ?: 0) + 1
            if (item.id == "batch:photo-2" && failFirstAttempt) error("temporary failure")
            "output-${item.id}"
        })
        processor.applyLook("batch", listOf("photo-1", "photo-2", "photo-3"), "look")
        failFirstAttempt = false

        processor.retry("batch:photo-2")

        assertEquals(mapOf("batch:photo-1" to 1, "batch:photo-2" to 2, "batch:photo-3" to 1), attempts)
        assertEquals(BatchItemState.COMPOSED, store.item("batch:photo-2").state)
    }

    @Test
    fun applyingLookCreatesAndComposesEveryPhotoOnce() = runBlocking {
        val store = FakeBatchQueueStore()
        val processed = mutableListOf<String>()

        BatchQueueProcessor(store, compose = { item ->
            processed += item.sourceId
            assertEquals("look-json", item.lookJson)
            "output-${item.id}"
        }).applyLook("batch", listOf("photo-1", "photo-2", "photo-3"), "look-json")

        assertEquals(listOf("photo-1", "photo-2", "photo-3"), processed.sorted())
        assertTrue(store.items.all { it.state == BatchItemState.COMPOSED })
    }

    private class FakeBatchQueueStore(
        items: List<BatchQueueItem> = emptyList(),
    ) : BatchQueueStore {
        val items = items.toMutableList()

        override suspend fun insertBatch(batchId: String, sourceIds: List<String>, lookJson: String) {
            sourceIds.distinct().forEach { sourceId ->
                val id = "$batchId:$sourceId"
                if (items.none { it.id == id }) {
                    items += BatchQueueItem(id, batchId, sourceId, lookJson, BatchItemState.QUEUED)
                }
            }
        }

        override suspend fun items(batchId: String): List<BatchQueueItem> =
            items.filter { it.batchId == batchId }

        override suspend fun recoverInterrupted(batchId: String) {
            items.replaceAll { item ->
                if (item.batchId == batchId && item.state == BatchItemState.PROCESSING) {
                    item.copy(state = BatchItemState.QUEUED)
                } else item
            }
        }

        override suspend fun claim(itemId: String): BatchQueueItem? {
            val index = items.indexOfFirst { it.id == itemId && it.state == BatchItemState.QUEUED }
            if (index < 0) return null
            return items[index].copy(state = BatchItemState.PROCESSING, attempt = items[index].attempt + 1)
                .also { items[index] = it }
        }

        override suspend fun complete(itemId: String, outputPath: String) {
            update(itemId) { it.copy(state = BatchItemState.COMPOSED, outputPath = outputPath, error = null) }
        }

        override suspend fun fail(itemId: String, message: String) {
            update(itemId) { it.copy(state = BatchItemState.FAILED, error = message) }
        }

        override suspend fun retry(itemId: String): String? {
            val index = items.indexOfFirst { it.id == itemId && it.state == BatchItemState.FAILED }
            if (index < 0) return null
            items[index] = items[index].copy(state = BatchItemState.QUEUED, error = null)
            return items[index].batchId
        }

        fun item(id: String): BatchQueueItem = items.single { it.id == id }

        private fun update(id: String, transform: (BatchQueueItem) -> BatchQueueItem) {
            val index = items.indexOfFirst { it.id == id }
            items[index] = transform(items[index])
        }
    }
}

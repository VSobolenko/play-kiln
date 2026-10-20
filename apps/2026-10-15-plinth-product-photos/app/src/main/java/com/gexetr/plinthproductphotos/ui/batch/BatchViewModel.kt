package com.gexetr.plinthproductphotos.ui.batch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gexetr.plinthproductphotos.data.batch.BatchDatabase
import com.gexetr.plinthproductphotos.data.batch.BatchRoomStore
import com.gexetr.plinthproductphotos.data.batch.BatchWorker
import com.gexetr.plinthproductphotos.domain.batch.BatchItemState
import com.gexetr.plinthproductphotos.domain.batch.BatchQueueItem
import com.gexetr.plinthproductphotos.domain.batch.selectBatchSources
import com.gexetr.plinthproductphotos.domain.model.Look
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatchUiState(
    val batchId: String,
    val selectedSourceIds: Set<String> = emptySet(),
    val isStarting: Boolean = false,
    val hasStarted: Boolean = false,
    val error: String? = null,
)

data class BatchWorkStatus(
    val batchId: String,
    val isScheduled: Boolean,
    val isRunning: Boolean,
    val isRetryScheduled: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BatchViewModel(
    private val context: Context,
    batchId: String,
    private val sourceIds: List<String>,
    private val lookJson: String,
) : ViewModel() {
    private val store = BatchRoomStore(BatchDatabase.get(context))
    private val _state = MutableStateFlow(BatchUiState(batchId, selectedSourceIds = sourceIds.toSet()))
    val state: StateFlow<BatchUiState> = _state.asStateFlow()
    val items: StateFlow<List<BatchQueueItem>> = _state
        .map { it.batchId }
        .distinctUntilChanged()
        .flatMapLatest(store::observeItems)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workStatus: StateFlow<BatchWorkStatus?> = _state
        .map { it.batchId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(BatchRoomStore.workName(id))
                .map { records ->
                    BatchWorkStatus(
                        batchId = id,
                        isScheduled = records.any { !it.state.isFinished },
                        isRunning = records.any { it.state == WorkInfo.State.RUNNING },
                        isRetryScheduled = records.any {
                            it.state == WorkInfo.State.ENQUEUED && it.runAttemptCount > 0
                        },
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun start() {
        if (_state.value.isStarting || _state.value.hasStarted) return
        val selected = selectBatchSources(sourceIds, _state.value.selectedSourceIds)
        if (selected.isEmpty()) return
        _state.update { it.copy(isStarting = true, hasStarted = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                store.insertBatch(_state.value.batchId, selected, lookJson)
                BatchWorker.enqueue(context, _state.value.batchId)
                _state.update { it.copy(isStarting = false, error = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update {
                    it.copy(
                        isStarting = false,
                        hasStarted = false,
                        error = failure.message?.takeIf(String::isNotBlank) ?: "Could not start batch",
                    )
                }
            }
        }
    }

    fun toggleSelection(sourceId: String) {
        if (sourceId !in sourceIds) return
        _state.update { current ->
            val selected = current.selectedSourceIds
            current.copy(
                selectedSourceIds = if (sourceId in selected) selected - sourceId else selected + sourceId,
            )
        }
    }

    fun selectAll() {
        _state.update { it.copy(selectedSourceIds = sourceIds.toSet()) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedSourceIds = emptySet()) }
    }

    fun retry(itemId: String) {
        if (_state.value.isStarting) return
        val batchId = _state.value.batchId
        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (store.retry(itemId) != null) {
                    BatchWorker.enqueue(context, batchId, appendIfRunning = true)
                }
                _state.update { it.copy(isStarting = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                setError(failure)
            }
        }
    }

    fun retryFailed() {
        if (_state.value.isStarting) return
        val batchId = _state.value.batchId
        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                store.items(batchId).filter { it.state == BatchItemState.FAILED }
                    .forEach { store.retry(it.id) }
                BatchWorker.enqueue(context, batchId, appendIfRunning = true)
                _state.update { it.copy(isStarting = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                setError(failure)
            }
        }
    }

    fun resume() {
        if (_state.value.isStarting) return
        val batchId = _state.value.batchId
        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BatchWorker.enqueue(context, batchId)
                _state.update { it.copy(isStarting = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                setError(failure)
            }
        }
    }

    fun pause() {
        if (_state.value.isStarting) return
        val workName = BatchRoomStore.workName(_state.value.batchId)
        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(workName).result.get()
                _state.update { it.copy(isStarting = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                setError(failure)
            }
        }
    }

    fun reapplyLook() {
        if (_state.value.isStarting) return
        val nextId = UUID.randomUUID().toString()
        _state.update { it.copy(batchId = nextId, hasStarted = false, error = null) }
        start()
    }

    private fun setError(failure: Exception) {
        _state.update {
            it.copy(
                isStarting = false,
                error = failure.message?.takeIf(String::isNotBlank) ?: "Batch action failed",
            )
        }
    }

    companion object {
        fun factory(
            context: Context,
            batchId: String,
            sourceIds: List<String>,
            look: Look,
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            val lookJson = com.gexetr.plinthproductphotos.domain.look.LookJsonCodec.encodeOne(look)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BatchViewModel(appContext, batchId, sourceIds, lookJson) as T
            }
        }
    }
}

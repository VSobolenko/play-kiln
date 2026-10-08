package com.gexetr.unitwalk.ui.compare

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.CompareOutcome
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.RoomDelta
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.compareRooms
import com.gexetr.unitwalk.domain.priorMoveInFor
import com.gexetr.unitwalk.domain.setDelta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Move-in versus move-out for one walk — the brain behind the `move-in-vs-move-out` promise.
 *
 * It resolves the prior report with [priorMoveInFor] (finished `MOVE_IN` for the same unit, never a
 * draft) and lays the two walks side by side with [compareRooms]. When there is no prior report the
 * outcome is [CompareOutcome.NoPriorReport] and **every** row's `priorCondition` is `null`: the
 * surface can say "no prior report" plainly, and no baseline is invented. Recording a row's
 * difference goes through `setDelta` and is persisted with [WalkStore.save] before the state is
 * republished, so a process kill cannot lose a recorded delta.
 *
 * The dispatcher is a constructor parameter for the same reason as the unlock ViewModel's: the store
 * already hops to `Dispatchers.IO` internally, so orchestration may run anywhere, and keeping it
 * injectable makes this class driveable off the main thread.
 */
class CompareViewModel(
    private val store: WalkStore,
    private val walkId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: CompareEvent) {
        when (event) {
            is CompareEvent.RecordDelta -> recordDelta(event.roomId, event.condition, event.note)
            CompareEvent.MessageDismissed -> _uiState.update { it.copy(message = null) }
        }
    }

    /** Re-reads the document and rebuilds the rows. Safe to call on resume. */
    fun refresh() {
        scope.launch {
            _uiState.value = buildState(store.load())
        }
    }

    private fun recordDelta(roomId: String, condition: RoomCondition, note: String) {
        scope.launch {
            val state = store.load()
            val current = state.walks.firstOrNull { it.id == walkId }
            if (current == null) {
                _uiState.value = buildState(state)
                return@launch
            }
            val updatedWalk = setDelta(
                current,
                RoomDelta(roomId = roomId, condition = condition, note = note),
            )
            val next = state.copy(
                walks = state.walks.map { walk ->
                    if (walk.id == walkId) updatedWalk else walk
                },
            )
            val saved = runCatching { store.save(next) }.isSuccess
            _uiState.value = buildState(next).copy(
                message = if (saved) CompareMessage.DeltaSaved else CompareMessage.SaveFailed,
            )
        }
    }

    private fun buildState(state: WalkState): CompareUiState {
        val current = state.walks.firstOrNull { it.id == walkId }
            ?: return CompareUiState(
                isLoaded = true,
                walkId = walkId,
                message = CompareMessage.WalkNotFound,
            )

        val prior = priorMoveInFor(state, current.unitId, current.id)
        val comparisons = compareRooms(prior, current)
        val conditionByRoomId = current.differences.associate { it.roomId to it.condition }

        // `compareRooms` returns one row per room of `current`, in the same order, so the zip is
        // positional and exact rather than a name lookup that could collide.
        val rows = current.rooms.zip(comparisons).map { (room, comparison) ->
            CompareRow(
                roomId = room.id,
                roomName = room.name,
                priorCondition = if (prior == null) null else comparison.priorCondition,
                currentCondition = conditionByRoomId[room.id],
                note = comparison.note,
            )
        }

        return CompareUiState(
            isLoaded = true,
            unitName = state.units.firstOrNull { it.id == current.unitId }?.name.orEmpty(),
            walkId = current.id,
            walkKind = current.kind,
            outcome = if (prior == null) CompareOutcome.NoPriorReport else CompareOutcome.HasPrior,
            priorWalkId = prior?.id,
            rows = rows,
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    companion object {
        /**
         * Builds the ViewModel over the app's own draft store. `WalkStore` takes a directory rather
         * than a `Context`, and its contract is that the app passes its private `filesDir` — the same
         * directory every other reader of the document uses.
         */
        fun factory(context: Context, walkId: String): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CompareViewModel(
                        store = WalkStore(applicationContext.filesDir),
                        walkId = walkId,
                    ) as T
            }
        }
    }
}

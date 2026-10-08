package com.gexetr.unitwalk.ui.walk

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.Photo
import com.gexetr.unitwalk.domain.PhotoSource
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.RoomDelta
import com.gexetr.unitwalk.domain.Walk
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.WalkStatus
import com.gexetr.unitwalk.domain.addPhoto
import com.gexetr.unitwalk.domain.finishRoom
import com.gexetr.unitwalk.domain.finishWalk
import com.gexetr.unitwalk.domain.openRoom
import com.gexetr.unitwalk.domain.setDelta
import com.gexetr.unitwalk.domain.setNote
import java.io.IOException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The room-by-room walk brain.
 *
 * ## The central promise: persist before the next room opens
 *
 * Every mutation goes through [mutate], which writes the next durable snapshot with
 * [WalkStore.save] **and only then** publishes it. Concretely, and deliberately in this order:
 *
 * 1. a new room is persisted before it becomes the current room ([addRoom]);
 * 2. a note is persisted as it is written ([setNoteForRoom]);
 * 3. photo bytes are written with [WalkStore.putPhoto] *before* the row that names them is committed
 *    ([attachPhoto]);
 * 4. a finished room is committed before the following room becomes current ([finishRoomAndAdvance]).
 *
 * Two rooms are **never** batched into one save: each event writes the document once. That ordering
 * is the product's whole reason to exist — a process killed between two events loses at most the
 * un-persisted step, and the store's atomic replace means it never sees half a document.
 *
 * ## Why this file carries [SavedStateHandle]
 *
 * The document on disk records the rooms, their notes and their photos, but *which room is open* is
 * transient UI position. [SavedStateHandle] holds it so a system-initiated process death restores
 * the landlord to the room he was in rather than to the last room in the list. It is written on
 * [restore] and on every [selectRoom], and it is read first on [restore]; when it holds nothing
 * (a fresh launch, a force stop) the walk falls back to its last room, which is the room being
 * worked on. Both keys are public so the navigation layer can seed [KEY_WALK_ID] from its argument.
 *
 * The `durability` family of the `crash-safe-photo-draft` promise is proved by this class: the walk
 * state, the current room id and the photo list all survive a fresh process because they are read
 * back from disk and from this handle.
 *
 * ## Testability
 *
 * [onEvent] is the non-suspending entry point a composable calls; [handle] is the same logic as a
 * suspending function, so a JVM test drives it inside `runBlocking` with no coroutine test runner.
 * The store, the handle and the clock are injected, and the scope is resolved lazily, so a test
 * never touches `Dispatchers.Main`.
 */
class WalkViewModel(
    private val store: WalkStore,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemUTC(),
    externalScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope: CoroutineScope by lazy { externalScope ?: viewModelScope }

    private var state: WalkState = WalkState()
    private var walk: Walk? = null

    private val _uiState = MutableStateFlow(WalkUiState())
    val uiState: StateFlow<WalkUiState> = _uiState.asStateFlow()

    fun onEvent(event: WalkEvent) {
        scope.launch { handle(event) }
    }

    /**
     * The same logic as a suspending function, so a JVM test can drive one event at a time inside
     * `runBlocking` with no coroutine test runner on the classpath. The app calls [onEvent].
     */
    suspend fun handle(event: WalkEvent) {
        when (event) {
            WalkEvent.ScreenShown -> restore()
            is WalkEvent.RequestWalk -> {
                savedStateHandle[KEY_WALK_ID] = event.walkId
                restore()
            }
            is WalkEvent.AddRoom -> addRoom(event.name)
            is WalkEvent.OpenRoom -> selectRoom(event.roomId)
            is WalkEvent.NoteChanged -> setNoteForRoom(event.roomId, event.note)
            is WalkEvent.ConditionChanged -> setConditionForRoom(event.roomId, event.condition)
            is WalkEvent.AttachPhoto -> attachPhoto(
                roomId = event.roomId,
                fileName = event.fileName,
                bytes = event.bytes,
                source = event.source,
            )
            is WalkEvent.FinishRoom -> finishRoomAndAdvance(event.roomId)
            WalkEvent.FinishWalk -> finishTheWalk()
            WalkEvent.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    /**
     * Loads the document and restores the position.
     *
     * [WalkStore.load] never throws — a corrupt document reads as an empty one — so a half-written
     * draft lands here as [WalkMessage.NothingToResume] rather than as a crash.
     */
    private suspend fun restore() {
        state = store.load()
        val requestedWalkId: String? = savedStateHandle[KEY_WALK_ID]
        val candidate = state.walks.firstOrNull { walk -> walk.id == requestedWalkId }
            ?: state.walks.lastOrNull { walk -> walk.status == WalkStatus.DRAFT }

        if (candidate == null) {
            walk = null
            _uiState.update { it.copy(isLoading = false, message = WalkMessage.NothingToResume) }
            return
        }

        walk = candidate
        savedStateHandle[KEY_WALK_ID] = candidate.id

        val storedRoomId: String? = savedStateHandle[KEY_ROOM_ID]
        val roomId = candidate.rooms.firstOrNull { room -> room.id == storedRoomId }?.id
            ?: candidate.rooms.lastOrNull()?.id
        if (roomId != null) savedStateHandle[KEY_ROOM_ID] = roomId

        publish(currentRoomId = roomId, message = null)
    }

    private suspend fun addRoom(rawName: String) {
        val current = walk ?: return nothingToResume()
        val name = rawName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(message = WalkMessage.RoomNameRequired) }
            return
        }
        // Derive the next snapshot, persist it, and only then make the new room current — the save
        // happens inside `mutate` before that function publishes anything.
        val next = openRoom(current, name)
        val newRoomId = next.rooms.last().id
        mutate(next, currentRoomId = newRoomId, message = WalkMessage.RoomAdded)
    }

    /**
     * Opens an existing room. This writes nothing: the room is already on disk. Only the transient
     * position changes, and it changes in the [SavedStateHandle] so a process kill restores it.
     */
    private fun selectRoom(roomId: String) {
        val current = walk ?: return nothingToResume()
        if (current.rooms.none { room -> room.id == roomId }) {
            _uiState.update { it.copy(message = WalkMessage.RoomNotFound) }
            return
        }
        savedStateHandle[KEY_ROOM_ID] = roomId
        publish(currentRoomId = roomId, message = null)
    }

    private suspend fun setNoteForRoom(roomId: String, note: String) {
        val current = walk ?: return nothingToResume()
        if (current.rooms.none { room -> room.id == roomId }) {
            _uiState.update { it.copy(message = WalkMessage.RoomNotFound) }
            return
        }
        val next = setNote(current, roomId, note)
        mutate(next, currentRoomId = _uiState.value.currentRoomId, message = WalkMessage.NoteSaved)
    }

    private suspend fun setConditionForRoom(roomId: String, condition: RoomCondition) {
        val current = walk ?: return nothingToResume()
        if (current.rooms.none { room -> room.id == roomId }) {
            _uiState.update { it.copy(message = WalkMessage.RoomNotFound) }
            return
        }
        // The delta keeps the note already recorded for the room; recording a condition must not
        // silently erase what the landlord wrote.
        val note = current.differences.firstOrNull { delta -> delta.roomId == roomId }?.note.orEmpty()
        val next = setDelta(current, RoomDelta(roomId = roomId, condition = condition, note = note))
        mutate(next, currentRoomId = _uiState.value.currentRoomId, message = WalkMessage.ConditionSaved)
    }

    private suspend fun attachPhoto(
        roomId: String,
        fileName: String,
        bytes: ByteArray,
        source: PhotoSource,
    ) {
        val current = walk ?: return nothingToResume()
        if (current.rooms.none { room -> room.id == roomId }) {
            _uiState.update { it.copy(message = WalkMessage.RoomNotFound) }
            return
        }
        val name = fileName.trim().ifEmpty { PhotoUris.newPhotoFileName() }

        // Bytes first: the committed row must never name a photo that is not already on disk.
        try {
            store.putPhoto(bytes, name)
        } catch (failure: IOException) {
            _uiState.update { it.copy(message = WalkMessage.NotSaved) }
            return
        }

        val photo = Photo(
            id = name,
            fileName = name,
            capturedAtIso = Instant.now(clock).toString(),
            source = source,
        )
        val next = addPhoto(current, roomId, photo)
        mutate(next, currentRoomId = _uiState.value.currentRoomId, message = WalkMessage.PhotoStored)
    }

    private suspend fun finishRoomAndAdvance(roomId: String) {
        val current = walk ?: return nothingToResume()
        val index = current.rooms.indexOfFirst { room -> room.id == roomId }
        if (index < 0) {
            _uiState.update { it.copy(message = WalkMessage.RoomNotFound) }
            return
        }
        val next = finishRoom(current, roomId)
        val followingRoomId = next.rooms.getOrNull(index + 1)?.id
        // The finished room is committed by `mutate` before the following room becomes current.
        mutate(next, currentRoomId = followingRoomId, message = WalkMessage.RoomFinished)
    }

    private suspend fun finishTheWalk() {
        val current = walk ?: return nothingToResume()
        val next = finishWalk(current, Instant.now(clock).toString())
        mutate(next, currentRoomId = _uiState.value.currentRoomId, message = WalkMessage.WalkFinished)
    }

    /**
     * Persists [next] and republishes the state — in that order, always.
     *
     * One event is one write: the whole document is replaced once per mutation and two rooms are
     * never batched. When the write fails the in-memory state is left on the last durable snapshot
     * and the failure is reported as [WalkMessage.NotSaved] rather than shown as a saved change.
     */
    private suspend fun mutate(next: Walk, currentRoomId: String?, message: WalkMessage): Boolean {
        val nextState = state.copy(
            walks = state.walks.map { existing -> if (existing.id == next.id) next else existing },
        )
        return try {
            store.save(nextState)
            state = nextState
            walk = next
            publish(currentRoomId = currentRoomId, message = message)
            true
        } catch (failure: IOException) {
            _uiState.update { it.copy(message = WalkMessage.NotSaved) }
            false
        }
    }

    private fun publish(currentRoomId: String?, message: WalkMessage?) {
        val current = walk
        if (current == null) {
            _uiState.update { it.copy(isLoading = false, message = message) }
            return
        }
        val conditionByRoom: Map<String, RoomCondition> =
            current.differences.associate { delta -> delta.roomId to delta.condition }
        _uiState.update {
            it.copy(
                isLoading = false,
                walkId = current.id,
                unitId = current.unitId,
                kind = current.kind,
                status = current.status,
                rooms = current.rooms.map { room ->
                    RoomRow(
                        id = room.id,
                        name = room.name,
                        note = room.note,
                        photoFileNames = room.photos.map { photo -> photo.fileName },
                        condition = conditionByRoom[room.id],
                    )
                },
                currentRoomId = currentRoomId,
                photoCount = current.rooms.sumOf { room -> room.photos.size },
                message = message,
            )
        }
    }

    private fun nothingToResume() {
        _uiState.update { it.copy(isLoading = false, message = WalkMessage.NothingToResume) }
    }

    companion object {
        /** The [SavedStateHandle] key holding the walk being edited. */
        const val KEY_WALK_ID: String = "walk.walkId"

        /** The [SavedStateHandle] key holding the room the landlord is in. */
        const val KEY_ROOM_ID: String = "walk.currentRoomId"

        /**
         * Builds the ViewModel with the app's real store and the navigation's saved state.
         *
         * [createSavedStateHandle] is what makes [SavedStateHandle] survive a system-initiated
         * process death: the values written here come back unchanged when Android restores the
         * destination, so the walk reopens at the room it was interrupted in.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    WalkViewModel(
                        store = WalkStore(applicationContext.filesDir),
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }
        }
    }
}

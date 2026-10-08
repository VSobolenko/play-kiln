package com.gexetr.unitwalk.ui.walk

import androidx.lifecycle.SavedStateHandle
import com.gexetr.unitwalk.data.WalkStore
import com.gexetr.unitwalk.domain.PhotoSource
import com.gexetr.unitwalk.domain.Room
import com.gexetr.unitwalk.domain.RoomCondition
import com.gexetr.unitwalk.domain.Unit
import com.gexetr.unitwalk.domain.Walk
import com.gexetr.unitwalk.domain.WalkKind
import com.gexetr.unitwalk.domain.WalkState
import com.gexetr.unitwalk.domain.WalkStatus
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The room-by-room walk brain, driven on the JVM.
 *
 * The central assertion is the product's promise: after each mutation the **document on disk**
 * already carries it, read back through a brand-new [WalkStore] so nothing can be satisfied from
 * in-memory state. Temporary directories live under `.kiln/scratch/` — never the system temp
 * directory — and `kotlinx-coroutines-test` is deliberately absent, so the suspending `handle` is
 * driven inside `runBlocking`.
 */
class WalkViewModelTest {

    private val tempDirs = mutableListOf<File>()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC)

    @After
    fun cleanUp() {
        tempDirs.forEach { dir -> dir.deleteRecursively() }
        tempDirs.clear()
    }

    // --- persist before the next room opens --------------------------------------------------

    @Test
    fun everyRoomMutationIsPersistedBeforeTheNextRoomOpens() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val viewModel = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        viewModel.handle(WalkEvent.RequestWalk("w1"))

        // Room 1 is durable before room 2 is asked for: a fresh store already sees it.
        viewModel.handle(WalkEvent.AddRoom("Kitchen"))
        assertEquals(listOf("Kitchen"), durableRooms(dir).map { room -> room.name })

        viewModel.handle(WalkEvent.AddRoom("Hall"))
        assertEquals(listOf("Kitchen", "Hall"), durableRooms(dir).map { room -> room.name })

        val kitchenId = viewModel.uiState.value.rooms.first { row -> row.name == "Kitchen" }.id

        viewModel.handle(WalkEvent.NoteChanged(kitchenId, "Chip on the worktop"))
        assertEquals("Chip on the worktop", durableRooms(dir).first { it.id == kitchenId }.note)

        // The bytes are on disk *and* the row that names them is committed, in that order.
        val fileName = PhotoUris.newPhotoFileName()
        val bytes = byteArrayOf(9, 8, 7, 6)
        viewModel.handle(WalkEvent.AttachPhoto(kitchenId, fileName, bytes, PhotoSource.CAMERA))
        val durableRoom = durableRooms(dir).first { it.id == kitchenId }
        assertEquals(listOf(fileName), durableRoom.photos.map { photo -> photo.fileName })
        assertArrayEquals(bytes, WalkStore(dir).photoFile(fileName).readBytes())

        viewModel.handle(WalkEvent.ConditionChanged(kitchenId, RoomCondition.WORN))
        val durable = WalkStore(dir).load().walks.single()
        val delta = durable.differences.first { it.roomId == kitchenId }
        assertEquals(RoomCondition.WORN, delta.condition)
        // The delta is a separate row from the room: recording the condition left the room's own
        // note alone, which is what the walk screen shows.
        assertEquals("Chip on the worktop", durable.rooms.first { it.id == kitchenId }.note)
    }

    @Test
    fun finishingARoomCommitsItBeforeTheFollowingRoomBecomesCurrent() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val viewModel = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        viewModel.handle(WalkEvent.RequestWalk("w1"))
        viewModel.handle(WalkEvent.AddRoom("Kitchen"))
        viewModel.handle(WalkEvent.AddRoom("Hall"))
        val kitchenId = viewModel.uiState.value.rooms.first { row -> row.name == "Kitchen" }.id

        viewModel.handle(WalkEvent.FinishRoom(kitchenId))

        // The whole walk is durable, and the following room is current only after that write.
        assertEquals(2, durableRooms(dir).size)
        assertEquals("Hall", viewModel.uiState.value.currentRoom?.name)
    }

    @Test
    fun aBlankRoomNameIsRejectedRatherThanCommitted() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val viewModel = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        viewModel.handle(WalkEvent.RequestWalk("w1"))

        viewModel.handle(WalkEvent.AddRoom("   "))

        assertEquals(WalkMessage.RoomNameRequired, viewModel.uiState.value.message)
        assertTrue(durableRooms(dir).isEmpty())
    }

    // --- a resumed draft restores position, notes and photos ---------------------------------

    @Test
    fun aResumedDraftRestoresTheSamePositionNoteAndPhotosFromDisk() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val first = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        first.handle(WalkEvent.RequestWalk("w1"))
        first.handle(WalkEvent.AddRoom("Kitchen"))
        first.handle(WalkEvent.AddRoom("Hall"))
        val hallId = first.uiState.value.rooms.first { row -> row.name == "Hall" }.id
        first.handle(WalkEvent.NoteChanged(hallId, "Scuff by the door"))
        val fileName = PhotoUris.newPhotoFileName()
        first.handle(WalkEvent.AttachPhoto(hallId, fileName, byteArrayOf(1, 2, 3), PhotoSource.PICKER))
        first.handle(WalkEvent.OpenRoom(hallId))

        // A process kill: a brand-new store, saved state and brain over the same directory.
        val resumed = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        resumed.handle(WalkEvent.ScreenShown)

        assertEquals("w1", resumed.uiState.value.walkId)
        assertEquals(listOf("Kitchen", "Hall"), resumed.uiState.value.rooms.map { row -> row.name })
        assertEquals("Hall", resumed.uiState.value.currentRoom?.name)
        assertEquals("Scuff by the door", resumed.uiState.value.currentRoom?.note)
        assertEquals(listOf(fileName), resumed.uiState.value.currentRoom?.photoFileNames)
        assertArrayEquals(byteArrayOf(1, 2, 3), WalkStore(dir).photoFile(fileName).readBytes())
    }

    @Test
    fun theSavedStateHandleKeepsTheCurrentRoomAcrossProcessDeath() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val handle = SavedStateHandle()
        val first = WalkViewModel(WalkStore(dir), handle, clock)
        first.handle(WalkEvent.RequestWalk("w1"))
        first.handle(WalkEvent.AddRoom("Kitchen"))
        first.handle(WalkEvent.AddRoom("Hall"))
        val kitchenId = first.uiState.value.rooms.first { row -> row.name == "Kitchen" }.id
        first.handle(WalkEvent.OpenRoom(kitchenId))

        // The handle genuinely carries the transient position, not just a comment about it.
        assertEquals("w1", handle.get<String>(WalkViewModel.KEY_WALK_ID))
        assertEquals(kitchenId, handle.get<String>(WalkViewModel.KEY_ROOM_ID))

        // Android hands the same saved state to the recreated ViewModel: Kitchen is restored even
        // though Hall is the last room on disk.
        val restored = WalkViewModel(WalkStore(dir), handle, clock)
        restored.handle(WalkEvent.ScreenShown)

        assertEquals(kitchenId, restored.uiState.value.currentRoomId)
        assertEquals("Kitchen", restored.uiState.value.currentRoom?.name)
    }

    // --- finish the walk ----------------------------------------------------------------------

    @Test
    fun finishingTheWalkPersistsTheFinishedStatus() = runBlocking {
        val dir = newTempDir()
        WalkStore(dir).save(seedState())
        val viewModel = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        viewModel.handle(WalkEvent.RequestWalk("w1"))
        viewModel.handle(WalkEvent.AddRoom("Kitchen"))

        viewModel.handle(WalkEvent.FinishWalk)

        val durable = WalkStore(dir).load().walks.single()
        assertEquals(WalkStatus.FINISHED, durable.status)
        assertNotNull(durable.finishedAtIso)
        assertEquals(WalkStatus.FINISHED, viewModel.uiState.value.status)
    }

    // --- a half-written document costs the document, not the brain ---------------------------

    @Test
    fun aHalfWrittenDraftReadsAsNothingToResumeInsteadOfThrowing() = runBlocking {
        val dir = newTempDir()
        File(dir, WalkStore.DOCUMENT_NAME).writeText("{\"walks\":[{\"id\":", Charsets.UTF_8)

        val viewModel = WalkViewModel(WalkStore(dir), SavedStateHandle(), clock)
        viewModel.handle(WalkEvent.ScreenShown)

        assertTrue(viewModel.uiState.value.rooms.isEmpty())
        assertEquals(WalkMessage.NothingToResume, viewModel.uiState.value.message)
    }

    // --- helpers -----------------------------------------------------------------------------

    /** Reads the rooms back through a brand-new store, so nothing comes from in-memory state. */
    private suspend fun durableRooms(dir: File): List<Room> =
        WalkStore(dir).load().walks.single().rooms

    private fun seedState(): WalkState = WalkState(
        units = listOf(Unit(id = "u1", name = "Flat 3B", address = "12 Oak Street")),
        walks = listOf(
            Walk(
                id = "w1",
                unitId = "u1",
                kind = WalkKind.MOVE_IN,
                startedAtIso = "2026-10-05T09:00:00Z",
                status = WalkStatus.DRAFT,
            ),
        ),
    )

    private fun newTempDir(): File {
        val dir = File.createTempFile("unitwalk-walk-", "", scratchRoot())
        assertTrue(dir.delete())
        assertTrue(dir.mkdirs())
        tempDirs += dir
        return dir
    }

    /**
     * The repository's `.kiln/scratch/`, found by walking up from the test working directory, so a
     * test never touches the machine's system temp directory. The fallback is still inside the
     * checked-out tree.
     */
    private fun scratchRoot(): File {
        var candidate: File? = File(".").absoluteFile
        var hops = 0
        while (candidate != null && hops < 12) {
            if (File(candidate, ".kiln").isDirectory) {
                val scratch = File(candidate, ".kiln/scratch")
                scratch.mkdirs()
                return scratch
            }
            candidate = candidate.parentFile
            hops++
        }
        val fallback = File(".kiln/scratch")
        fallback.mkdirs()
        return fallback
    }
}

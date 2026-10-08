package com.gexetr.unitwalk.domain

import com.gexetr.unitwalk.data.WalkStore
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The draft store's crash-safety contract, driven directly on the JVM.
 *
 * Every temporary directory is created under the repository's disposable `.kiln/scratch/` — never
 * the system temp directory — and the tests use the store's blocking core so no coroutine test
 * runner is needed.
 */
class WalkStoreTest {

    private val tempDirs = mutableListOf<File>()

    @After
    fun cleanUp() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
    }

    @Test
    fun aSnapshotAndItsPhotoSurviveANewStoreOverTheSameDirectory() {
        val dir = newTempDir()
        val photoBytes = byteArrayOf(1, 2, 3, 4, 5)

        val first = WalkStore(dir)
        first.saveBlocking(sampleState())
        first.putPhotoBlocking(photoBytes, "room-kitchen-1.jpg")

        // Simulate a process kill and relaunch: a brand-new store, no shared in-memory state.
        val relaunched = WalkStore(dir)
        val loaded = relaunched.loadBlocking()

        assertEquals(sampleState(), loaded)
        val walk = loaded.walks.single()
        assertEquals(listOf("Kitchen", "Hall"), walk.rooms.map { it.name })
        assertEquals("Chip on the worktop", walk.rooms[0].note)
        assertEquals("Scuff by the door", walk.rooms[1].note)
        assertEquals(listOf("room-kitchen-1.jpg"), walk.rooms[0].photos.map { it.fileName })
        assertArrayEquals(photoBytes, relaunched.photoFile("room-kitchen-1.jpg").readBytes())
    }

    @Test
    fun aTruncatedDocumentReadsAsEmptyInsteadOfThrowingAndTheStoreStaysUsable() {
        val dir = newTempDir()
        val store = WalkStore(dir)
        store.saveBlocking(sampleState())

        // A kill during an in-place write leaves half a document behind.
        File(dir, WalkStore.DOCUMENT_NAME).writeText(
            "{ \"units\": [ { \"id\": \"u1\",",
            Charsets.UTF_8,
        )

        val recovered = WalkStore(dir).loadBlocking()
        assertEquals(WalkState(), recovered)

        // The store keeps working after the corrupt read.
        val next = sampleState().copy(version = 2)
        WalkStore(dir).saveBlocking(next)
        assertEquals(next, WalkStore(dir).loadBlocking())
    }

    @Test
    fun aMissingDocumentReadsAsEmpty() {
        assertEquals(WalkState(), WalkStore(newTempDir()).loadBlocking())
    }

    @Test
    fun anEmptyDocumentReadsAsEmpty() {
        val dir = newTempDir()
        File(dir, WalkStore.DOCUMENT_NAME).writeText("", Charsets.UTF_8)
        assertEquals(WalkState(), WalkStore(dir).loadBlocking())
    }

    private fun newTempDir(): File {
        val dir = File.createTempFile("walkstore-", "", scratchRoot())
        assertTrue(dir.delete())
        assertTrue(dir.mkdirs())
        tempDirs += dir
        return dir
    }

    /**
     * The repository's `.kiln/scratch/`, found by walking up from the test working directory. The
     * fallback is still inside the checked-out tree; the system temp directory is never used.
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

    private fun sampleState(): WalkState = WalkState(
        units = listOf(Unit(id = "u1", name = "Flat 3B", address = "12 Oak Street")),
        walks = listOf(
            Walk(
                id = "w1",
                unitId = "u1",
                kind = WalkKind.MOVE_IN,
                startedAtIso = "2026-10-05T10:00:00Z",
                status = WalkStatus.DRAFT,
                rooms = listOf(
                    Room(
                        id = "r1",
                        name = "Kitchen",
                        note = "Chip on the worktop",
                        photos = listOf(
                            Photo(
                                id = "p1",
                                fileName = "room-kitchen-1.jpg",
                                capturedAtIso = "2026-10-05T10:05:00Z",
                                source = PhotoSource.CAMERA,
                            ),
                        ),
                    ),
                    Room(
                        id = "r2",
                        name = "Hall",
                        note = "Scuff by the door",
                        photos = listOf(
                            Photo(
                                id = "p2",
                                fileName = "room-hall-1.jpg",
                                capturedAtIso = "2026-10-05T10:08:00Z",
                                source = PhotoSource.PICKER,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )
}

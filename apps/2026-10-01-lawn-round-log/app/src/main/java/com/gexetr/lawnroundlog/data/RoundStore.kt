package com.gexetr.lawnroundlog.data

import com.gexetr.lawnroundlog.domain.RoundState
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The whole record, wrapped in the version that describes its shape.
 *
 * The version is inside the document rather than in its file name, because the file name is the
 * one thing a future reader cannot change and a version it cannot read is a record it cannot
 * migrate. A document whose version is newer than this build understands is refused rather than
 * guessed at.
 */
@Serializable
private data class RoundDocument(
    val schemaVersion: Int = RoundStore.SCHEMA_VERSION,
    val state: RoundState = RoundState(),
)

/**
 * One versioned JSON document in the app's private files directory.
 *
 * This is the only thing in the app that touches the operator's stored record, and it is written
 * so that no failure in it can cost him the record:
 *
 * - **One value, one document.** [RoundState] serialises whole, so the store, the CSV export and
 *   the tests agree on one shape and "the record survives" is one code path rather than three
 *   converters that can disagree.
 * - **The write is atomic.** The bytes go to a sibling temporary file in the same directory, are
 *   flushed to the device, and only then is the temporary file renamed over the real one. A
 *   process killed mid-save therefore leaves either the previous document or the new one — never a
 *   half-written file, which is the failure that turns a crash into data loss.
 * - **The read never throws.** A missing file, an unreadable file and a file that is not this
 *   document all read as an empty [RoundState]. The operator's alternative to a tolerant read is
 *   an app that will not open, and an app that will not open cannot be used to recover anything.
 *
 * Reads and writes are serialised on the instance, because the save and the load of one document
 * must not interleave.
 */
class RoundStore(private val file: File) {

    private val json = Json {
        // The defaults are written explicitly: a document that omits them would be read back with
        // values this build happens to default to, which is a silent dependency on the reader.
        encodeDefaults = true
        // A document written by a newer build that added a field still reads here.
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * The stored record, or an empty one when there is nothing usable at [file].
     *
     * A corrupt file is not repaired, deleted or reported to the caller: it is treated as an empty
     * record so the app opens, and the file itself is left on disk untouched, because it is the
     * only copy of whatever the operator had and a future build may be able to read it.
     */
    @Synchronized
    fun load(): RoundState {
        val text = try {
            if (!file.isFile) return RoundState()
            file.readText()
        } catch (_: Exception) {
            return RoundState()
        }
        return try {
            val document = json.decodeFromString(RoundDocument.serializer(), text)
            if (document.schemaVersion > SCHEMA_VERSION) RoundState() else document.state
        } catch (_: Exception) {
            RoundState()
        }
    }

    /**
     * Replace the stored record with [state].
     *
     * A failure here is allowed to surface: a save that could not be written is a fact the operator
     * has to be told, and swallowing it would be the app claiming to have kept a record it did not
     * keep. The previous document is untouched in that case, because nothing is renamed until the
     * new bytes are durably on disk.
     */
    @Synchronized
    fun save(state: RoundState) {
        val parent = file.parentFile
        parent?.mkdirs()
        val temporary = File(parent, file.name + TEMPORARY_SUFFIX)
        val bytes = json.encodeToString(RoundDocument.serializer(), RoundDocument(state = state))
            .toByteArray(Charsets.UTF_8)
        FileOutputStream(temporary).use { stream ->
            stream.write(bytes)
            stream.flush()
            // The rename is only atomic with respect to a crash if the bytes are already on the
            // device, so the flush is the durability half of the atomicity claim.
            stream.fd.sync()
        }
        moveIntoPlace(temporary)
    }

    private fun moveIntoPlace(temporary: File) {
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            // Still one rename rather than a truncate-and-rewrite, which is the part that matters.
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        /** The document shape this build writes and the newest one it can read. */
        const val SCHEMA_VERSION: Int = 1

        private const val TEMPORARY_SUFFIX = ".tmp"
    }
}

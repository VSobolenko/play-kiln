package com.gexetr.unitwalk.data

import com.gexetr.unitwalk.domain.WalkState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The walk draft store: one versioned JSON document plus the photo files, under a directory the
 * caller owns.
 *
 * Taking a [File] rather than a `Context` is what makes this class **JVM-testable**: the app passes
 * its private `filesDir`, and a unit test passes a temporary directory under `.kiln/scratch/`. The
 * store never chooses a location itself.
 *
 * ## Why the writes are shaped this way
 *
 * - **One document.** The whole portfolio is one [WalkState] at `walk-state.json`. A walk is far
 *   below a megabyte of notes, so "the walk survives a process kill" is one code path rather than a
 *   store-per-walk matrix.
 * - **Atomic replace.** [saveBlocking] writes `walk-state.json.tmp`, flushes and `fsync`s it, then
 *   renames it over the real name. The real document is never truncated in place, so a kill during
 *   a write leaves either the previous good document or the new one — never half of each.
 * - **Photo bytes before the row.** [putPhotoBlocking] writes and `fsync`s the photo file; the
 *   caller commits the JSON row that names it only afterwards. A crash can therefore leave an
 *   orphaned file, which costs disk, but can never leave a row pointing at a file that is not there.
 * - **Tolerant read.** A missing, empty, truncated or unparseable document reads as an empty
 *   [WalkState] — "nothing to resume" — and never throws. A corrupt draft must cost the draft, not
 *   the app.
 *
 * ## Honest limit
 *
 * The rename is atomic on the platforms that support it. On Windows, `File.renameTo` refuses to
 * replace an existing file, so the fallback removes the destination first; the window between the
 * delete and the rename is the one non-atomic step, and it is documented here rather than hidden.
 */
class WalkStore(
    private val dir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    private val document: File get() = File(dir, DOCUMENT_NAME)
    private val photosDir: File get() = File(dir, PHOTOS_DIR)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
    }

    /** Reads the current document; a missing or unreadable file reads as an empty state. */
    suspend fun load(): WalkState = withContext(io) { loadBlocking() }

    /** Atomically replaces the document with [state]. */
    suspend fun save(state: WalkState) {
        withContext(io) { saveBlocking(state) }
    }

    /**
     * Writes [bytes] to `photos/<fileName>` and returns the file.
     *
     * Must be awaited **before** the JSON row naming the photo is saved, so a committed row can
     * never reference a photo that is not on disk.
     */
    suspend fun putPhoto(bytes: ByteArray, fileName: String): File =
        withContext(io) { putPhotoBlocking(bytes, fileName) }

    /** The path a photo with [fileName] is stored at. The file may or may not exist yet. */
    fun photoFile(fileName: String): File = File(photosDir, safeName(fileName))

    // --- Blocking core, exposed as `internal` so JVM unit tests need no coroutine test runner. ---

    internal fun loadBlocking(): WalkState {
        return try {
            if (!document.isFile) return WalkState()
            val text = document.readText(Charsets.UTF_8)
            if (text.isBlank()) return WalkState()
            json.decodeFromString(WalkState.serializer(), text)
        } catch (cause: Exception) {
            // Truncated JSON, a wrong encoding, an unreadable file: all mean "nothing to resume".
            WalkState()
        }
    }

    internal fun saveBlocking(state: WalkState) {
        dir.mkdirs()
        val text = json.encodeToString(WalkState.serializer(), state)
        val tmp = File(dir, "$DOCUMENT_NAME.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        replaceAtomically(tmp, document)
    }

    internal fun putPhotoBlocking(bytes: ByteArray, fileName: String): File {
        photosDir.mkdirs()
        val target = File(photosDir, safeName(fileName))
        val tmp = File(photosDir, "${safeName(fileName)}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        replaceAtomically(tmp, target)
        return target
    }

    /**
     * Renames [tmp] over [target], falling back to a delete-then-rename on filesystems (Windows)
     * where `renameTo` will not replace an existing file.
     */
    private fun replaceAtomically(tmp: File, target: File) {
        if (tmp.renameTo(target)) return
        if (target.exists() && !target.delete()) {
            throw IOException("could not replace ${target.name}")
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    /** Drops any directory components so a photo name can never escape the store's `photos/` dir. */
    private fun safeName(fileName: String): String {
        val name = File(fileName).name
        return if (name.isBlank() || name == "." || name == "..") {
            "photo-${System.nanoTime()}"
        } else {
            name
        }
    }

    companion object {
        /** The single versioned document inside [dir]. */
        const val DOCUMENT_NAME: String = "walk-state.json"

        /** The subdirectory holding photo bytes. */
        const val PHOTOS_DIR: String = "photos"
    }
}

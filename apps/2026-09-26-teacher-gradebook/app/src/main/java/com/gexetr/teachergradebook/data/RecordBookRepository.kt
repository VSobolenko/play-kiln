package com.gexetr.teachergradebook.data

import com.gexetr.teachergradebook.domain.BackupCodec
import com.gexetr.teachergradebook.domain.model.RecordBook
import java.io.File

/**
 * The record as one file on the device.
 *
 * A single document rather than a database on purpose: the internal store, the backup file and the
 * test fixtures are then one format, so "back up everything and get it back" is one code path
 * instead of a converter per direction. A teacher's record (a few hundred students over a few dozen
 * assignments) stays far below a megabyte, so a database would add migration code without adding a
 * capability.
 *
 * Writes are atomic — a temporary file is written and then renamed over the target — because the
 * one thing that must never happen to a gradebook is a half-written file: a truncated document is
 * an unreadable record, and it would fail exactly when the teacher has been working longest.
 */
interface RecordBookRepository {
    /** Reads the stored record, or an empty one on a first run. Never throws. */
    fun load(): RecordBook

    /** Writes the record. Returns false when the write did not reach the disk. */
    fun save(book: RecordBook): Boolean
}

class FileRecordBookRepository(
    private val file: File,
) : RecordBookRepository {

    override fun load(): RecordBook {
        if (!file.isFile) return RecordBook()
        val text = try {
            file.readText()
        } catch (error: Exception) {
            return RecordBook()
        }
        return when (val result = BackupCodec.decode(text)) {
            is BackupCodec.DecodeResult.Ok -> result.book
            // A damaged store must not be silently replaced by an empty one, so the unreadable
            // file is preserved beside itself and a fresh record is used. The teacher can still
            // recover the original, and nothing they typed is destroyed by the act of opening.
            is BackupCodec.DecodeResult.Rejected -> {
                runCatching { file.copyTo(File(file.parentFile, "${file.name}.damaged"), overwrite = true) }
                RecordBook()
            }
        }
    }

    override fun save(book: RecordBook): Boolean = try {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(BackupCodec.encode(book))
        if (temporary.renameTo(file)) {
            true
        } else {
            // A rename can fail when the target exists on some filesystems; the copy is the
            // fallback rather than an error, and the temporary file is cleaned up either way.
            val copied = runCatching {
                file.writeText(temporary.readText())
                true
            }.getOrDefault(false)
            temporary.delete()
            copied
        }
    } catch (error: Exception) {
        false
    }
}

package com.gexetr.teachergradebook.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

/**
 * Reading and writing the files a teacher picks.
 *
 * Everything goes through the Storage Access Framework, which is what lets this app request no
 * permission at all: the teacher choosing a file *is* the grant, for that one file. A
 * `READ_EXTERNAL_STORAGE` permission would have bought the ability to read every file on the
 * device, including other people's, in exchange for a Play declaration and a review question.
 */
class DocumentStore(private val context: Context) {

    /** Reads a file the teacher picked. Returns null with a reason rather than throwing. */
    fun readText(uri: String): Result<String> = runCatching {
        val parsed = uri.toUri()
        context.contentResolver.openInputStream(parsed)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw DocumentStoreException(DocumentFailure.CouldNotOpen)
    }

    /** Overwrites a file the teacher created. */
    fun writeText(uri: String, text: String): Result<Unit> = runCatching {
        val parsed = uri.toUri()
        context.contentResolver.openOutputStream(parsed, "wt")?.use { stream ->
            stream.bufferedWriter().use { writer -> writer.write(text) }
        } ?: throw DocumentStoreException(DocumentFailure.CouldNotOpenForWriting)
    }

    fun writeBytes(uri: String, bytes: ByteArray): Result<Unit> = runCatching {
        val parsed = uri.toUri()
        context.contentResolver.openOutputStream(parsed, "wt")?.use { stream ->
            stream.write(bytes)
        } ?: throw DocumentStoreException(DocumentFailure.CouldNotOpenForWriting)
    }

    /** A human-readable name for a picked location, for the confirmation message. */
    fun displayName(uri: String): String? {
        val parsed = Uri.parse(uri)
        return parsed.lastPathSegment?.substringAfterLast('/')
    }

    companion object {
        const val MIME_CSV = "text/csv"
        const val MIME_JSON = "application/json"

        /** The record lives in app-private storage, which needs no permission and no picker. */
        fun recordFile(context: Context): File = File(context.filesDir, "recordbook.json")
    }
}

enum class DocumentFailure { CouldNotOpen, CouldNotOpenForWriting }

class DocumentStoreException(val reason: DocumentFailure) : IllegalStateException()

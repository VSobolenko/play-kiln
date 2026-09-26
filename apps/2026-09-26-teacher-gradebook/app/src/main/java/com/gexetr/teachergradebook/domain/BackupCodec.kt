package com.gexetr.teachergradebook.domain

import com.gexetr.teachergradebook.domain.model.RecordBook
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The backup format, which is also the internal store format.
 *
 * One format for both is the whole point: "back up everything and get it back" is then a single
 * code path with a single set of tests, instead of a converter per direction that can drift.
 *
 * A restore never half-applies. The schema version is read from the raw document before any
 * decoding, so a file from another build is refused with a reason that names both versions, and the
 * teacher's current record is left exactly as it was.
 */
object BackupCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        // Unknown keys are an error on purpose: a document this build does not fully understand is
        // a document it must not silently trim down to the fields it happens to know.
        ignoreUnknownKeys = false
    }

    sealed interface DecodeResult {
        data class Ok(val book: RecordBook) : DecodeResult

        data class Rejected(val reason: BackupRejection) : DecodeResult
    }

    const val SUGGESTED_FILE_NAME = "mark-book-record.json"

    fun encode(book: RecordBook): String = json.encodeToString(RecordBook.serializer(), book)

    fun decode(text: String): DecodeResult {
        if (text.isBlank()) {
            return DecodeResult.Rejected(BackupRejection.Empty)
        }

        val element = try {
            json.parseToJsonElement(text)
        } catch (error: Exception) {
            return DecodeResult.Rejected(BackupRejection.NotMarkBookBackup)
        }

        val root = element as? JsonObject
            ?: return DecodeResult.Rejected(BackupRejection.NotMarkBookBackup)

        val version = (root["schemaVersion"] as? JsonPrimitive)?.content?.toIntOrNull()
            ?: return DecodeResult.Rejected(BackupRejection.MissingSchemaVersion)

        if (version != RecordBook.SCHEMA_VERSION) {
            return DecodeResult.Rejected(
                BackupRejection.UnsupportedVersion(version, RecordBook.SCHEMA_VERSION),
            )
        }

        return try {
            DecodeResult.Ok(json.decodeFromJsonElement(RecordBook.serializer(), root))
        } catch (error: Exception) {
            DecodeResult.Rejected(BackupRejection.Damaged(error.message))
        }
    }
}

sealed interface BackupRejection {
    data object Empty : BackupRejection
    data object NotMarkBookBackup : BackupRejection
    data object MissingSchemaVersion : BackupRejection
    data class UnsupportedVersion(val fileVersion: Int, val supportedVersion: Int) : BackupRejection
    data class Damaged(val detail: String?) : BackupRejection
}

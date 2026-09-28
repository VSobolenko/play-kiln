package com.gexetr.pipesizing.ui.cases

import com.gexetr.pipesizing.ui.storage.ENTRY_SEPARATOR
import com.gexetr.pipesizing.ui.storage.FIELD_SEPARATOR
import com.gexetr.pipesizing.ui.storage.KEY_VALUE_SEPARATOR
import com.gexetr.pipesizing.ui.storage.RECORD_SEPARATOR
import com.gexetr.pipesizing.ui.storage.escapeField
import com.gexetr.pipesizing.ui.storage.unescapeField
import java.util.UUID

/**
 * One case the user chose to keep.
 *
 * [fields] is the same key/value shape the calculator already persists between runs, so a saved
 * case and the working case cannot drift into two different formats: loading one is exactly the
 * restore the app already performs on launch.
 */
data class SavedCase(
    val id: String,
    val name: String,
    val savedAtEpochMillis: Long,
    val fields: Map<String, String>,
) {
    companion object {
        fun new(name: String, fields: Map<String, String>, nowEpochMillis: Long): SavedCase =
            SavedCase(id = UUID.randomUUID().toString(), name = name, savedAtEpochMillis = nowEpochMillis, fields = fields)
    }
}

/**
 * The storage codec for the saved-case list.
 *
 * Deliberately not JSON: the app needs no JSON dependency, and a tolerant line format that drops an
 * unreadable record is safer here than a parser that throws and loses every case because one was
 * truncated. Separators inside a user-typed name are escaped, so a name containing the separator
 * cannot split a record.
 *
 * The escaping rule is `ui/storage/LineCodec.kt`'s and is imported, never restated: the table codec
 * uses the same one, and two hand-written copies of an escape table is the defect
 * `.cursor/rules/verification-discipline.mdc` names under "One source of truth, imported".
 */
internal object SavedCaseCodec {

    fun encode(cases: List<SavedCase>): String = cases.joinToString(RECORD_SEPARATOR.toString()) { case ->
        val fields = case.fields.entries
            .sortedBy { it.key }
            .joinToString(ENTRY_SEPARATOR.toString()) { (key, value) ->
                escapeField(key) + KEY_VALUE_SEPARATOR + escapeField(value)
            }
        listOf(
            escapeField(case.id),
            escapeField(case.name),
            case.savedAtEpochMillis.toString(),
            fields,
        ).joinToString(FIELD_SEPARATOR.toString())
    }

    fun decode(text: String): List<SavedCase> {
        if (text.isBlank()) return emptyList()
        return text.split(RECORD_SEPARATOR).mapNotNull(::decodeRecord)
    }

    private fun decodeRecord(record: String): SavedCase? {
        val parts = record.split(FIELD_SEPARATOR)
        if (parts.size != 4) return null

        val timestamp = parts[2].toLongOrNull() ?: return null
        val fields = mutableMapOf<String, String>()
        if (parts[3].isNotEmpty()) {
            parts[3].split(ENTRY_SEPARATOR).forEach { entry ->
                val separator = entry.indexOf(KEY_VALUE_SEPARATOR)
                if (separator <= 0) return@forEach
                val key = unescapeField(entry.substring(0, separator))
                val value = unescapeField(entry.substring(separator + 1))
                if (key.isNotEmpty()) fields[key] = value
            }
        }

        return SavedCase(
            id = unescapeField(parts[0]),
            name = unescapeField(parts[1]),
            savedAtEpochMillis = timestamp,
            fields = fields,
        )
    }
}

package com.gexetr.pipesizing.ui.cases

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
 */
internal object SavedCaseCodec {

    private const val RECORD_SEPARATOR = '\n'
    private const val FIELD_SEPARATOR = '|'
    private const val ENTRY_SEPARATOR = ';'
    private const val KEY_VALUE_SEPARATOR = '='

    fun encode(cases: List<SavedCase>): String = cases.joinToString(RECORD_SEPARATOR.toString()) { case ->
        val fields = case.fields.entries
            .sortedBy { it.key }
            .joinToString(ENTRY_SEPARATOR.toString()) { (key, value) ->
                escape(key) + KEY_VALUE_SEPARATOR + escape(value)
            }
        listOf(
            escape(case.id),
            escape(case.name),
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
                val key = unescape(entry.substring(0, separator))
                val value = unescape(entry.substring(separator + 1))
                if (key.isNotEmpty()) fields[key] = value
            }
        }

        return SavedCase(
            id = unescape(parts[0]),
            name = unescape(parts[1]),
            savedAtEpochMillis = timestamp,
            fields = fields,
        )
    }

    /**
     * Escapes the three characters that carry structure. Backslash is escaped first, so an escaped
     * separator can never be re-read as a real one.
     */
    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                FIELD_SEPARATOR -> append("\\p")
                ENTRY_SEPARATOR -> append("\\s")
                KEY_VALUE_SEPARATOR -> append("\\e")
                RECORD_SEPARATOR -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character != '\\' || index == value.lastIndex) {
                append(character)
                index += 1
                continue
            }
            when (val escaped = value[index + 1]) {
                '\\' -> append('\\')
                'p' -> append(FIELD_SEPARATOR)
                's' -> append(ENTRY_SEPARATOR)
                'e' -> append(KEY_VALUE_SEPARATOR)
                'n' -> append(RECORD_SEPARATOR)
                'r' -> append('\r')
                else -> append(escaped)
            }
            index += 2
        }
    }
}

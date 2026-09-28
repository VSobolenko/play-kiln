package com.gexetr.pipesizing.ui.storage

internal const val RECORD_SEPARATOR = '\n'
internal const val FIELD_SEPARATOR = '|'
internal const val ENTRY_SEPARATOR = ';'
internal const val KEY_VALUE_SEPARATOR = '='

internal fun escapeField(value: String): String = buildString(value.length) {
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

internal fun unescapeField(value: String): String = buildString(value.length) {
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

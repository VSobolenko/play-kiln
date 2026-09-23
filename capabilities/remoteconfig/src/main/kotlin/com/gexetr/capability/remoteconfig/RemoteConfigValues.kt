package com.gexetr.capability.remoteconfig

import java.nio.charset.StandardCharsets
import java.util.Locale

internal fun Any.toRemoteBooleanOrNull(): Boolean? = when (this) {
    is Boolean -> this
    is String -> when (lowercase(Locale.US)) {
        "1", "true", "t", "yes", "y", "on" -> true
        "0", "false", "f", "no", "n", "off", "" -> false
        else -> null
    }
    else -> null
}

internal fun Any.toRemoteLongOrNull(): Long? = when (this) {
    is Long -> this
    is String -> toLongOrNull()
    else -> null
}

internal fun Any.toRemoteString(): String = when (this) {
    is ByteArray -> String(this, StandardCharsets.UTF_8)
    else -> toString()
}

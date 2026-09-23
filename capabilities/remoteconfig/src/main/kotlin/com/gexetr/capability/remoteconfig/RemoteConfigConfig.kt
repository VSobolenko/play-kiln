package com.gexetr.capability.remoteconfig

/**
 * Fetch policy and in-app values used before Firebase has returned a remote configuration.
 *
 * Defaults accept the same value types as Firebase Remote Config: [ByteArray], [Boolean],
 * [Double], [Long], and [String]. Keeping a local copy lets controllers serve defaults
 * synchronously, including when Firebase is absent from a debug build.
 */
data class RemoteConfigConfig(
    val minimumFetchIntervalSeconds: Long = DEFAULT_FETCH_INTERVAL_SECONDS,
    val defaults: Map<String, Any> = emptyMap(),
) {
    /** Problems that make this configuration unusable. Empty means valid. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()

        if (minimumFetchIntervalSeconds < 0) {
            problems += "remote config fetch interval must not be negative"
        }

        defaults.forEach { (key, value) ->
            if (key.isBlank()) {
                problems += "remote config default keys must not be blank"
            }
            if (value !is ByteArray && value !is Boolean && value !is Double &&
                value !is Long && value !is String
            ) {
                problems += "remote config default '$key' has an unsupported value type"
            }
            if (value is Double && !value.isFinite()) {
                problems += "remote config default '$key' must be finite"
            }
        }

        return problems
    }

    companion object {
        const val DEFAULT_FETCH_INTERVAL_SECONDS = 12 * 60 * 60L
    }
}

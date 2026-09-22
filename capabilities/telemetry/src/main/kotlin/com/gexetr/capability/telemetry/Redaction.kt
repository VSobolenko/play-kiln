package com.gexetr.capability.telemetry

/**
 * Removes values that must never reach a log sink, a crash report or an event stream.
 *
 * This is a safety net, not a licence: never pass user content, tokens, e-mail addresses or
 * free-form user text to telemetry in the first place. [scrub] exists because a factory ships
 * many apps and one careless `log("$request")` should not leak anything.
 */
object Redaction {

    private const val EMAIL = "[REDACTED_EMAIL]"
    private const val DIGITS = "[REDACTED_DIGITS]"
    private const val VALUE = "[REDACTED]"

    // Matches addr-spec style addresses, including plus-addressing and dotted domains.
    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    // Matches `token=…`, `api_key=…`, `password=…` and friends, stopping at the first
    // separator so that `?token=abc&next=home` only loses the token.
    private val credentialRegex = Regex(
        "(?i)\\b(token|access[_-]?token|refresh[_-]?token|api[_-]?key|key|password|passwd|secret|auth|authorization)\\s*=\\s*([^\\s&?#]+)",
    )

    // Phone numbers, account ids, advertising ids and similar identifiers.
    private val digitRunRegex = Regex("\\d{7,}")

    /**
     * Returns [message] with e-mail addresses, credential values and long digit runs masked.
     * A message without sensitive fragments is returned unchanged.
     */
    fun scrub(message: String): String {
        var result = emailRegex.replace(message, EMAIL)
        result = credentialRegex.replace(result) { match -> "${match.groupValues[1]}=$VALUE" }
        result = digitRunRegex.replace(result, DIGITS)
        return result
    }

    /** Scrubs every value of an event payload, returning an immutable copy. */
    fun scrub(params: Map<String, String>): Map<String, String> =
        if (params.isEmpty()) params else params.mapValues { (_, value) -> scrub(value) }
}

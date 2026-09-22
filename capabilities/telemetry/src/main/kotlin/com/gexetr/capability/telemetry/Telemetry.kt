package com.gexetr.capability.telemetry

import com.gexetr.capability.core.Clock
import java.util.ArrayDeque

/**
 * PII-safe logging and product-event surface.
 *
 * Contract for every implementation and every caller:
 *
 * - `message` is for a human reading a log; it must never contain user content, identifiers
 *   (account id, advertising id, e-mail, phone) or tokens. [Redaction] masks common mistakes,
 *   but the caller is responsible.
 * - `params` keys must be screen/action names such as `screen=home`, `action=open_settings`.
 *   Never pass free-form user text, file names containing user data, or anything that could
 *   identify a person.
 *
 * The default implementation shipped by this capability is [NoOpTelemetry]: a slot collects
 * nothing until it explicitly opts in, which keeps the Play Data safety answer honest.
 */
interface Telemetry {

    fun log(level: Level, tag: String, message: String, error: Throwable? = null)

    fun event(name: String, params: Map<String, String> = emptyMap())

    enum class Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }
}

/** Ships as the default: it records nothing and sends nothing. */
object NoOpTelemetry : Telemetry {
    override fun log(level: Telemetry.Level, tag: String, message: String, error: Throwable?) = Unit

    override fun event(name: String, params: Map<String, String>) = Unit
}

/**
 * Bounded in-memory recorder.
 *
 * Intended for unit tests, debug builds and bug-report capture. It never persists anything and
 * never grows without bound, so it is safe to enable in a debug build; it must not be used as
 * the production sink for a shipped app.
 */
class RecordingTelemetry(
    private val capacity: Int = 256,
    private val clock: Clock = Clock.System,
) : Telemetry {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    sealed interface Entry {
        val timestampMillis: Long

        data class Log(
            override val timestampMillis: Long,
            val level: Telemetry.Level,
            val tag: String,
            val message: String,
            val errorType: String?,
        ) : Entry

        data class Event(
            override val timestampMillis: Long,
            val name: String,
            val params: Map<String, String>,
        ) : Entry
    }

    private val lock = Any()
    private val buffer = ArrayDeque<Entry>(capacity)

    override fun log(level: Telemetry.Level, tag: String, message: String, error: Throwable?) {
        record(
            Entry.Log(
                timestampMillis = clock.nowMillis(),
                level = level,
                tag = tag,
                message = Redaction.scrub(message),
                // The message may leak; the class name never does.
                errorType = error?.javaClass?.name,
            ),
        )
    }

    override fun event(name: String, params: Map<String, String>) {
        record(
            Entry.Event(
                timestampMillis = clock.nowMillis(),
                name = name,
                params = Redaction.scrub(params),
            ),
        )
    }

    /** Oldest-first snapshot of what has been recorded since construction. */
    fun entries(): List<Entry> = synchronized(lock) { buffer.toList() }

    fun clear() = synchronized(lock) { buffer.clear() }

    private fun record(entry: Entry) = synchronized(lock) {
        if (buffer.size == capacity) buffer.removeFirst()
        buffer.addLast(entry)
    }
}

package com.gexetr.capability.telemetry

import com.gexetr.capability.core.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryTest {

    private class FakeClock(private var value: Long = 1_000L) : Clock {
        override fun nowMillis(): Long = value

        fun advance(delta: Long) {
            value += delta
        }
    }

    @Test
    fun `no op telemetry records nothing`() {
        NoOpTelemetry.log(Telemetry.Level.ERROR, "tag", "boom", IllegalStateException("x"))
        NoOpTelemetry.event("screen_view", mapOf("screen" to "home"))

        // Nothing to assert beyond "it did not throw and keeps no state".
        assertTrue(NoOpTelemetry === NoOpTelemetry)
    }

    @Test
    fun `recording telemetry keeps entries in order with timestamps`() {
        val clock = FakeClock()
        val telemetry = RecordingTelemetry(clock = clock)

        telemetry.log(Telemetry.Level.INFO, "app", "started")
        clock.advance(500)
        telemetry.event("screen_view", mapOf("screen" to "home"))

        val entries = telemetry.entries()
        assertEquals(2, entries.size)
        assertEquals(1_000L, entries[0].timestampMillis)
        assertEquals(1_500L, entries[1].timestampMillis)

        val log = entries[0] as RecordingTelemetry.Entry.Log
        assertEquals(Telemetry.Level.INFO, log.level)
        assertEquals("app", log.tag)
        assertEquals("started", log.message)

        val event = entries[1] as RecordingTelemetry.Entry.Event
        assertEquals("screen_view", event.name)
        assertEquals("home", event.params["screen"])
    }

    @Test
    fun `recording telemetry evicts the oldest entry beyond capacity`() {
        val telemetry = RecordingTelemetry(capacity = 3)

        repeat(5) { index -> telemetry.event("tick", mapOf("index" to index.toString())) }

        val entries = telemetry.entries()
        assertEquals(3, entries.size)
        assertEquals(
            listOf("2", "3", "4"),
            entries.map { (it as RecordingTelemetry.Entry.Event).params["index"] },
        )
    }

    @Test
    fun `recording telemetry scrubs messages and parameters`() {
        val telemetry = RecordingTelemetry()

        telemetry.log(Telemetry.Level.WARN, "net", "failed for jane@example.com token=abc123")
        telemetry.event("checkout", mapOf("account" to "998877665544"))

        val log = telemetry.entries()[0] as RecordingTelemetry.Entry.Log
        assertEquals("failed for [REDACTED_EMAIL] token=[REDACTED]", log.message)

        val event = telemetry.entries()[1] as RecordingTelemetry.Entry.Event
        assertEquals("[REDACTED_DIGITS]", event.params["account"])
    }

    @Test
    fun `recording telemetry keeps only the error type, never its message`() {
        val telemetry = RecordingTelemetry()

        telemetry.log(
            Telemetry.Level.ERROR,
            "net",
            "request failed",
            IllegalStateException("token=abc123 leaked"),
        )

        val log = telemetry.entries()[0] as RecordingTelemetry.Entry.Log
        assertEquals(IllegalStateException::class.java.name, log.errorType)
        assertTrue(log.errorType?.contains("leaked") != true)
    }

    @Test
    fun `clear empties the buffer`() {
        val telemetry = RecordingTelemetry()
        telemetry.event("screen_view")
        telemetry.clear()

        assertEquals(0, telemetry.entries().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `capacity must be positive`() {
        RecordingTelemetry(capacity = 0)
    }
}

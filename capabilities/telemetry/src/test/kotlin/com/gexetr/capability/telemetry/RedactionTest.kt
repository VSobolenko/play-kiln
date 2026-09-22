package com.gexetr.capability.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class RedactionTest {
    @Test
    fun `scrub masks email addresses`() {
        assertEquals(
            "Contact [REDACTED_EMAIL] today",
            Redaction.scrub("Contact jane.doe+test@example.co.uk today"),
        )
    }

    @Test
    fun `scrub masks digit runs of seven or more digits`() {
        assertEquals(
            "account [REDACTED_DIGITS] and code 123456",
            Redaction.scrub("account 1234567890 and code 123456"),
        )
    }

    @Test
    fun `scrub masks credential fragments case insensitively`() {
        assertEquals(
            "token=[REDACTED] key=[REDACTED] PASSWORD=[REDACTED] Secret=[REDACTED]",
            Redaction.scrub("token=abc123 key=xyz PASSWORD=hunter2 Secret=s3cr3t"),
        )
    }

    @Test
    fun `scrub stops credential masking at query separators`() {
        assertEquals(
            "token=[REDACTED]&next=home",
            Redaction.scrub("token=abc123&next=home"),
        )
    }

    @Test
    fun `scrub leaves non-sensitive messages unchanged`() {
        val message = "screen=home action=open"

        assertEquals(message, Redaction.scrub(message))
    }
}

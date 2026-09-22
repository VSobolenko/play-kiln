package com.gexetr.capability.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlayKilnResultTest {
    @Test
    fun `map transforms a success value`() {
        val result = PlayKilnResult.Success(21).map { it * 2 }

        assertEquals(PlayKilnResult.Success(42), result)
    }

    @Test
    fun `map preserves a failure`() {
        val failure = PlayKilnResult.Failure(PlayKilnResult.Reason.NETWORK)

        val result = failure.map { value: Int -> value * 2 }

        assertSame(failure, result)
    }

    @Test
    fun `getOrNull returns a success value`() {
        assertEquals("ready", PlayKilnResult.Success("ready").getOrNull())
    }

    @Test
    fun `getOrNull returns null for a failure`() {
        val failure = PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)

        assertNull(failure.getOrNull())
    }
}

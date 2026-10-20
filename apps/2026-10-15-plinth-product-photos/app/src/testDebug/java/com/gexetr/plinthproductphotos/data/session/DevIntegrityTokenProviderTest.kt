package com.gexetr.plinthproductphotos.data.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DevIntegrityTokenProviderTest {
    @Test
    fun suppliesExactDevTokenForBase64UrlSha256() = runBlocking {
        val hash = "k5zfdib4y8jP6BbPX5-_tBxb3oTTz6K6WfiVyzZLXIc"
        assertEquals("dev:$hash", DevIntegrityTokenProvider().token(hash))
        assertEquals("dev:${"_".repeat(43)}", DevIntegrityTokenProvider().token("_".repeat(43)))
    }

    @Test
    fun rejectsMalformedHashes() = runBlocking {
        val provider = DevIntegrityTokenProvider()
        val invalidHashes = listOf(
            "",
            "a".repeat(42),
            "a".repeat(44),
            "a".repeat(42) + "=",
            "a".repeat(42) + "+",
            "a".repeat(42) + "/",
            "a".repeat(42) + "\n",
        )
        for (hash in invalidHashes) {
            try {
                provider.token(hash)
                fail("Accepted malformed request hash")
            } catch (error: IntegrityTokenException) {
                assertEquals(SessionFailure.INTEGRITY, error.reason)
            }
        }
    }
}

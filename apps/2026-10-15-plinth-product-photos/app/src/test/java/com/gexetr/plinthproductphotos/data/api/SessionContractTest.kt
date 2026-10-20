package com.gexetr.plinthproductphotos.data.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionContractTest {
    @Test
    fun sessionSerializationPreservesEveryRequiredServerField() {
        val wire = """{"installId":"install-1","appVersion":"0.1.0","locale":"ru-RU","integrityToken":"signed-token","challengeId":"12345678-1234-1234-1234-123456789abc","challenge":"server-nonce"}"""
        val request = ApiJson.decodeFromString<SessionRequest>(wire)
        assertEquals(
            ApiJson.parseToJsonElement(wire),
            ApiJson.parseToJsonElement(ApiJson.encodeToString(request)),
        )
    }

    @Test
    fun challengeSerializationPreservesTheLocaleThatWillBeAttested() {
        val request = ApiJson.decodeFromString<IntegrityChallengeRequest>(
            """{"installId":"install-1","appVersion":"0.1.0","locale":"ru-RU"}""",
        )
        val body = ApiJson.parseToJsonElement(ApiJson.encodeToString(request)).jsonObject
        assertEquals("ru-RU", body["locale"]?.jsonPrimitive?.content)
    }
}

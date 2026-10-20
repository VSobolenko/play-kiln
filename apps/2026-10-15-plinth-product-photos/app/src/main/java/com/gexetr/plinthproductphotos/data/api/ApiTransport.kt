package com.gexetr.plinthproductphotos.data.api

import java.io.IOException

data class TransportRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray? = null,
)

data class TransportResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

interface ApiTransport {
    @Throws(IOException::class)
    fun execute(request: TransportRequest): TransportResponse
}

package com.gexetr.plinthproductphotos.data.api

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpApiTransport(
    private val client: OkHttpClient = OkHttpClient(),
) : ApiTransport {
    override fun execute(request: TransportRequest): TransportResponse {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val requestBody = request.body?.toRequestBody(
            request.headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                ?.value?.toMediaTypeOrNull(),
        )
        builder.method(request.method, requestBody)
        try {
            client.newCall(builder.build()).execute().use { response ->
                return TransportResponse(
                    statusCode = response.code,
                    headers = response.headers.toMultimap().mapValues { (_, values) -> values.joinToString(",") },
                    body = response.body?.bytes() ?: ByteArray(0),
                )
            }
        } catch (failure: IOException) {
            throw failure
        }
    }
}

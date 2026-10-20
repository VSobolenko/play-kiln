package com.gexetr.plinthproductphotos.domain.cutout

import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.ApiTransport
import com.gexetr.plinthproductphotos.data.api.JobRequest
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.data.api.TransportRequest
import com.gexetr.plinthproductphotos.data.api.TransportResponse
import java.security.MessageDigest
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EraseUploadTest {
    @Test
    fun maskUploadsAsPngWithItsDigestAndEraseSendsPhotoThenMask() {
        val sent = mutableListOf<TransportRequest>()
        val client = ApiClient("https://unit.test/v1", object : ApiTransport {
            override fun execute(request: TransportRequest): TransportResponse {
                sent += request
                return if (request.method == "PUT") {
                    TransportResponse(201, emptyMap(), """{"uploadId":"up-${sent.size}","expiresAt":9999999999999}""".toByteArray())
                } else {
                    TransportResponse(202, emptyMap(), """{"jobId":"job-1","reserved":3,"state":"QUEUED"}""".toByteArray())
                }
            }
        })
        val png = EraseMask.encodePng(byteArrayOf(0, 0xff.toByte()), 2, 1)
        val photo = client.uploadPhoto(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1), "image/jpeg", "t") as ApiResult.Success
        val mask = client.uploadMask(png, "t") as ApiResult.Success
        assertTrue(client.submitErase("req-1", photo.value.uploadId, mask.value.uploadId, 3, "t") is ApiResult.Success)

        assertEquals("image/png", sent[1].headers["Content-Type"])
        val digest = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) }
        assertEquals(digest, sent[1].headers["X-Client-Sha256"])
        val job = ApiJson.decodeFromString<JobRequest>(requireNotNull(sent[2].body).toString(Charsets.UTF_8))
        assertEquals(OperationId.ERASE, job.op)
        assertEquals(listOf("up-1", "up-2"), job.uploadIds)
    }
}

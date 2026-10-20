package com.gexetr.plinthproductphotos.data.api

import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {
    @Test
    fun imageToolsSendTheSelectedUploadIdAndBytesWithoutAddingOtherImages() {
        val selectedBytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 17, 73)
        val operations = listOf(
            OperationId.CUTOUT, OperationId.GENERATED_SCENE, OperationId.RELIGHT,
            OperationId.EXPAND, OperationId.RECOLOUR,
        )
        operations.forEach { operation ->
            val sent = mutableListOf<TransportRequest>()
            val selectedUploadId = "selected-phone-${operation.name}"
            val client = ApiClient("https://unit.test/v1", object : ApiTransport {
                override fun execute(request: TransportRequest): TransportResponse {
                    sent += request
                    return if (request.method == "PUT") {
                        TransportResponse(
                            201, emptyMap(),
                            """{"uploadId":"$selectedUploadId","expiresAt":9999999999999}""".toByteArray(),
                        )
                    } else {
                        TransportResponse(
                            202, emptyMap(),
                            """{"jobId":"job-selected","reserved":2,"state":"QUEUED"}""".toByteArray(),
                        )
                    }
                }
            })
            val upload = client.uploadPhoto(selectedBytes, "image/jpeg", "device-token") as ApiResult.Success
            val request = JobRequest(
                clientRequestId = "selected-${operation.name}", op = operation,
                uploadIds = listOf(upload.value.uploadId), params = JsonObject(emptyMap()), estimateCredits = 2,
            )
            assertTrue(client.submitJob(request, "device-token") is ApiResult.Success)
            assertTrue(selectedBytes.contentEquals(sent[0].body))
            val decoded = ApiJson.decodeFromString<JobRequest>(requireNotNull(sent[1].body).toString(Charsets.UTF_8))
            assertEquals(operation, decoded.op)
            assertEquals(listOf(selectedUploadId), decoded.uploadIds)
            assertEquals(JsonObject(emptyMap()), decoded.params)
        }
    }

    @Test
    fun recognitionSendsOneRawJpegAndDecodesTheClosedSchema() {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2)
        var request: TransportRequest? = null
        var calls = 0
        val client = ApiClient(
            "https://unit.test/v1",
            object : ApiTransport {
                override fun execute(sent: TransportRequest): TransportResponse {
                    calls++
                    request = sent
                    return TransportResponse(
                        statusCode = 200,
                        headers = emptyMap(),
                        body = """{"category":"bag","confidence":0.87,"attributes":["leather"],"visibleText":["SKU-4"],"scaleHint":{"kind":"hand"},"sku":"SKU-4"}"""
                            .toByteArray(StandardCharsets.UTF_8),
                    )
                }
            },
        )

        val result = client.recognizePhoto(RecognitionRequest(jpeg), "device-token")

        assertEquals(1, calls)
        assertTrue(result is ApiResult.Success)
        val recognition = (result as ApiResult.Success).value
        assertEquals(RecognitionCategory.BAG, recognition.category)
        assertEquals(0.87, recognition.confidence, 0.0)
        assertEquals(listOf("leather"), recognition.attributes)
        assertEquals(listOf("SKU-4"), recognition.visibleText)
        assertEquals(RecognitionScaleKind.HAND, recognition.scaleHint.kind)
        assertEquals("SKU-4", recognition.sku)
        assertEquals("POST", request?.method)
        assertEquals("https://unit.test/v1/recognize", request?.url)
        assertTrue(request?.headers?.get("Authorization")?.startsWith("Bearer ") == true)
        assertEquals("image/jpeg", request?.headers?.get("Content-Type"))
        assertTrue(jpeg.contentEquals(request?.body))
    }

    @Test
    fun mapsEveryDocumentedErrorAndKeepsUnknownCodesTyped() {
        val documented = mapOf(
            "UNAUTHORIZED" to ApiFailure.Unauthorized,
            "DEVICE_BLOCKED" to ApiFailure.DeviceBlocked,
            "INSUFFICIENT_CREDITS" to ApiFailure.InsufficientCredits,
            "PRICE_CHANGED" to ApiFailure.PriceChanged,
            "PURCHASE_INVALID" to ApiFailure.PurchaseInvalid,
            "PURCHASE_PENDING" to ApiFailure.PurchasePending,
            "RATE_LIMITED" to ApiFailure.RateLimited,
            "CAP_REACHED" to ApiFailure.CapReached,
            "KILL_SWITCH" to ApiFailure.KillSwitch,
            "TOO_LARGE" to ApiFailure.TooLarge,
            "UNSUPPORTED_MEDIA" to ApiFailure.UnsupportedMedia,
            "VERSION_TOO_OLD" to ApiFailure.VersionTooOld,
            "PROVIDER_FAILED" to ApiFailure.ProviderFailed,
            "GUARD_REJECTED" to ApiFailure.GuardRejected,
            "MODERATION_BLOCKED" to ApiFailure.ModerationBlocked,
        )

        documented.forEach { (code, expected) ->
            val envelope = """{"error":{"code":"$code","message":"server message","retryAfterSec":4,"details":{"credits":2}}}"""
            val client = ApiClient(
                "https://unit.test/v1",
                StaticResponseTransport(
                    TransportResponse(402, emptyMap(), envelope.toByteArray(StandardCharsets.UTF_8)),
                ),
            )
            val result = client.getWallet("device-token")
            assertEquals(
                code,
                expected,
                (result as ApiResult.Failure).failure,
            )
            assertEquals(4, result.envelope?.retryAfterSec)
            assertEquals(buildJsonObject { put("credits", 2) }, result.envelope?.details)
        }
        val unknown = """{"error":{"code":"FUTURE_CODE","message":"server message","retryAfterSec":4,"details":{"extra":true}}}"""
        val unknownResult = ApiClient(
            "https://unit.test/v1",
            StaticResponseTransport(
                TransportResponse(599, emptyMap(), unknown.toByteArray(StandardCharsets.UTF_8)),
            ),
        ).getWallet("device-token") as ApiResult.Failure
        assertEquals(
            ApiFailure.Unknown(
                "FUTURE_CODE",
                "server message",
                4,
                buildJsonObject { put("extra", true) },
            ),
            unknownResult.failure,
        )
    }

    @Test
    fun retryAfterLostJobResponseReusesClientRequestIdAndReturnsExistingJob() {
        val transport = LostFirstJobResponseTransport()
        val client = ApiClient("https://unit.test/v1", transport)
        val request = JobRequest(
            clientRequestId = "request-stable-1",
            op = OperationId.CUTOUT,
            uploadIds = listOf("upload-1"),
            params = JsonObject(emptyMap()),
            estimateCredits = 1,
        )

        val first = client.submitJob(request, "device-token")
        val second = client.submitJob(request, "device-token")

        assertTrue(first is ApiResult.Failure && first.failure is ApiFailure.Network)
        assertEquals(ApiResult.Success(JobAccepted("job-existing", 1, JobState.QUEUED)), second)
        assertEquals(listOf("request-stable-1", "request-stable-1"), transport.requestIds)
        assertEquals(listOf("Bearer device-token", "Bearer device-token"), transport.authorizationHeaders)
        assertEquals(1, transport.jobsCreated)
    }

    @Test
    fun failedJobWithZeroChargeIsExposedAsNotCharged() {
        val transport = StaticResponseTransport(
            TransportResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = """{"state":"FAILED","charged":0,"reserved":2,"released":2,"error":{"code":"PROVIDER_FAILED","message":"timeout"}}"""
                    .toByteArray(StandardCharsets.UTF_8),
            ),
        )
        val client = ApiClient("https://unit.test/v1", transport)

        val result = client.pollJob("job-1", "device-token")

        assertTrue(result is ApiResult.Success)
        val status = (result as ApiResult.Success).value
        assertEquals(JobState.FAILED, status.state)
        assertTrue(status.notCharged)
        assertEquals(2, status.reserved)
        assertEquals(2, status.released)
        assertEquals(ApiJobErrorCode.PROVIDER_FAILED, status.error?.typedCode)
    }

    @Test
    fun failedJobWithUnreleasedReservationIsNotReportedAsRefunded() {
        val transport = StaticResponseTransport(
            TransportResponse(
                statusCode = 200,
                headers = emptyMap(),
                body = """{"state":"FAILED","charged":0,"reserved":2,"released":0}"""
                    .toByteArray(StandardCharsets.UTF_8),
            ),
        )

        val status = (ApiClient("https://unit.test/v1", transport).pollJob("job-1", "device-token")
            as ApiResult.Success).value

        assertEquals(false, status.notCharged)
    }

    private class StaticResponseTransport(
        private val response: TransportResponse,
    ) : ApiTransport {
        override fun execute(request: TransportRequest): TransportResponse = response
    }

    private class LostFirstJobResponseTransport : ApiTransport {
        val requestIds = mutableListOf<String>()
        val authorizationHeaders = mutableListOf<String?>()
        var jobsCreated = 0
            private set
        private var existingJob: JobAccepted? = null

        override fun execute(request: TransportRequest): TransportResponse {
            val submitted = ApiJson.decodeFromString<JobRequest>(
                String(requireNotNull(request.body), StandardCharsets.UTF_8),
            )
            requestIds += submitted.clientRequestId
            authorizationHeaders += request.headers["Authorization"]
            val accepted = existingJob ?: JobAccepted("job-existing", submitted.estimateCredits, JobState.QUEUED)
                .also {
                    existingJob = it
                    jobsCreated += 1
                }
            if (requestIds.size == 1) throw IOException("response lost after server acceptance")
            return TransportResponse(
                statusCode = 202,
                headers = emptyMap(),
                body = ApiJson.encodeToString(accepted).toByteArray(StandardCharsets.UTF_8),
            )
        }
    }
}

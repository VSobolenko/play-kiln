package com.gexetr.plinthproductphotos.data.api

import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import java.util.logging.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

data class DeliveredResult(
    val bytes: ByteArray,
    val deliveryAttemptId: String,
    val sha256: String,
)

class ApiClient(
    baseUrl: String = API_BASE_URL,
    private val transport: ApiTransport = OkHttpApiTransport(),
) {
    private val root = baseUrl.trimEnd('/')

    fun getIntegrityChallenge(
        request: IntegrityChallengeRequest,
    ): ApiResult<IntegrityChallengeResponse> = json(
        method = "POST",
        path = "/integrity/challenge",
        body = request,
        token = null,
        successCode = 200,
    )

    fun createSession(request: SessionRequest): ApiResult<SessionResponse> = json(
        method = "POST",
        path = "/session",
        body = request,
        token = null,
        successCode = 200,
    )

    fun getConfig(): ApiResult<ApiConfigResponse> = json(
        method = "GET",
        path = "/config",
        body = null,
        token = null,
        successCode = 200,
    )

    fun getWallet(deviceToken: String): ApiResult<WalletResponse> = json(
        method = "GET",
        path = "/wallet",
        body = null,
        token = deviceToken,
        successCode = 200,
    )

    fun uploadPhoto(bytes: ByteArray, mimeType: String, deviceToken: String): ApiResult<UploadResponse> {
        val digest = sha256(bytes)
        return json(
            method = "PUT",
            path = "/uploads",
            body = bytes,
            token = deviceToken,
            successCode = 201,
            additionalHeaders = mapOf(
                "Content-Type" to mimeType,
                "X-Client-Sha256" to digest,
            ),
        )
    }

    /** Uploads an erase mask (grayscale PNG; white = erase, black = keep). */
    fun uploadMask(pngBytes: ByteArray, deviceToken: String): ApiResult<UploadResponse> =
        uploadPhoto(pngBytes, "image/png", deviceToken)

    /** O-12 takes exactly two uploads, in this order: the photo, then its mask. */
    fun submitErase(
        clientRequestId: String,
        photoUploadId: String,
        maskUploadId: String,
        credits: Int,
        deviceToken: String,
    ): ApiResult<JobAccepted> = submitJob(
        JobRequest(
            clientRequestId = clientRequestId,
            op = OperationId.ERASE,
            uploadIds = listOf(photoUploadId, maskUploadId),
            params = JsonObject(emptyMap()),
            estimateCredits = credits,
        ),
        deviceToken,
    )

    fun recognizePhoto(
        request: RecognitionRequest,
        deviceToken: String,
    ): ApiResult<RecognitionResponse> {
        require(
            request.jpegBytes.size >= 2 &&
                request.jpegBytes[0] == 0xff.toByte() &&
                request.jpegBytes[1] == 0xd8.toByte(),
        ) { "Recognition requires a JPEG image" }
        return json(
            method = "POST",
            path = "/recognize",
            body = request.jpegBytes,
            token = deviceToken,
            successCode = 200,
            additionalHeaders = mapOf(
                "Content-Type" to "image/jpeg",
                "X-Client-Sha256" to sha256(request.jpegBytes),
            ),
        )
    }

    fun submitJob(request: JobRequest, deviceToken: String): ApiResult<JobAccepted> = json(
        method = "POST",
        path = "/jobs",
        body = request,
        token = deviceToken,
        successCode = 202,
    )

    fun submitCutout(
        clientRequestId: String,
        uploadId: String,
        credits: Int,
        deviceToken: String,
    ): ApiResult<JobAccepted> = submitJob(
        JobRequest(
            clientRequestId = clientRequestId,
            op = OperationId.CUTOUT,
            uploadIds = listOf(uploadId),
            params = JsonObject(emptyMap()),
            estimateCredits = credits,
        ),
        deviceToken,
    )

    fun pollJob(jobId: String, deviceToken: String): ApiResult<JobStatus> = json(
        method = "GET",
        path = "/jobs/${jobId.encodePathSegment()}",
        body = null,
        token = deviceToken,
        successCode = 200,
    )

    fun downloadResult(
        jobId: String,
        result: JobResult,
        deviceToken: String,
    ): ApiResult<DeliveredResult> {
        val response = when (val execution = execute(
            TransportRequest(
                method = "GET",
                url = "$root/jobs/${jobId.encodePathSegment()}/result",
                headers = buildMap {
                    put("Authorization", "Bearer $deviceToken")
                    put("X-Result-Token", result.resultToken)
                },
            ),
        )) {
            is TransportExecution.Response -> execution.response
            is TransportExecution.NetworkFailure -> return ApiResult.Failure(
                ApiFailure.Network(execution.message, execution.category),
            )
        }
        if (response.statusCode !in 200..299) return parseApiError(response.statusCode, response.body)
        val attemptId = response.header("X-Delivery-Attempt")
            ?: return invalidResponse("MISSING_DELIVERY_ATTEMPT")
        val bytes = response.body
        val declaredLength = response.header("Content-Length")?.toLongOrNull()
            ?: return invalidResponse("MISSING_CONTENT_LENGTH")
        if (bytes.size.toLong() != result.bytes || declaredLength != result.bytes) {
            return invalidResponse("RESULT_LENGTH_MISMATCH")
        }
        val actualDigest = sha256(bytes)
        val responseDigest = response.header("Digest")
            ?.substringAfter("sha-256=", missingDelimiterValue = "")
            ?.takeIf(String::isNotEmpty)
            ?: return invalidResponse("MISSING_RESULT_DIGEST")
        if (!result.sha256.matches(Regex("[a-fA-F0-9]{64}"))) {
            return invalidResponse("INVALID_EXPECTED_DIGEST")
        }
        val expectedDigestBase64 = Base64.getEncoder().encodeToString(hexToBytes(result.sha256))
        if (actualDigest != result.sha256.lowercase() || responseDigest != expectedDigestBase64) {
            return invalidResponse("RESULT_DIGEST_MISMATCH")
        }
        return ApiResult.Success(DeliveredResult(bytes, attemptId, actualDigest))
    }

    fun submitDeliveryReceipt(
        jobId: String,
        receipt: DeliveryReceipt,
        deviceToken: String,
    ): ApiResult<DeliveryReceiptResponse> = json(
        method = "POST",
        path = "/jobs/${jobId.encodePathSegment()}/delivery-receipts",
        body = receipt,
        token = deviceToken,
        successCode = 200,
    )

    fun cancelJob(jobId: String, deviceToken: String): ApiResult<CancelResponse> = json(
        method = "POST",
        path = "/jobs/${jobId.encodePathSegment()}/cancel",
        body = null,
        token = deviceToken,
        successCode = 200,
    )

    private inline fun <reified T> json(
        method: String,
        path: String,
        body: Any?,
        token: String?,
        successCode: Int,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        val encodedBody = when (body) {
            null -> null
            is ByteArray -> body
            else -> encodeRequestBody(body).toByteArray(Charsets.UTF_8)
        }
        val headers = buildMap {
            if (token != null) put("Authorization", "Bearer $token")
            if (body != null && body !is ByteArray) put("Content-Type", "application/json")
            putAll(additionalHeaders)
        }
        val response = when (val execution = execute(TransportRequest(method, "$root$path", headers, encodedBody))) {
            is TransportExecution.Response -> execution.response
            is TransportExecution.NetworkFailure -> return ApiResult.Failure(
                ApiFailure.Network(execution.message, execution.category),
            )
        }
        if (response.statusCode != successCode) return parseApiError(response.statusCode, response.body)
        return try {
            ApiResult.Success(ApiJson.decodeFromString<T>(response.body.toString(Charsets.UTF_8)))
        } catch (_: SerializationException) {
            invalidResponse("INVALID_SUCCESS_BODY")
        } catch (_: IllegalArgumentException) {
            invalidResponse("INVALID_SUCCESS_BODY")
        }
    }

    private fun execute(request: TransportRequest): TransportExecution =
        try {
            TransportExecution.Response(transport.execute(request))
        } catch (failure: IOException) {
            val category = failure.javaClass.simpleName
            val message = failure.message
            logger.warning("API transport $category: ${message.orEmpty()}")
            TransportExecution.NetworkFailure(category, message)
        }

    private sealed interface TransportExecution {
        data class Response(val response: TransportResponse) : TransportExecution
        data class NetworkFailure(val category: String, val message: String?) : TransportExecution
    }

    private fun invalidResponse(code: String): ApiResult.Failure = ApiResult.Failure(
        ApiFailure.Unknown(
            code = code,
            message = "The server response did not match the expected format.",
            retryAfterSec = null,
            details = JsonObject(emptyMap()),
        ),
    )

    private companion object {
        val logger: Logger = Logger.getLogger(ApiClient::class.java.name)
    }

    private fun encodeRequestBody(body: Any): String = when (body) {
        is IntegrityChallengeRequest -> ApiJson.encodeToString(body)
        is SessionRequest -> ApiJson.encodeToString(body)
        is JobRequest -> ApiJson.encodeToString(body)
        is DeliveryReceipt -> ApiJson.encodeToString(body)
        else -> throw IllegalArgumentException("Unsupported API request body type")
    }

    private fun String.encodePathSegment(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun hexToBytes(value: String): ByteArray {
        require(value.length == 64 && value.all { it.digitToIntOrNull(16) != null })
        return ByteArray(32) { index ->
            val high = value[index * 2].digitToInt(16)
            val low = value[index * 2 + 1].digitToInt(16)
            ((high shl 4) or low).toByte()
        }
    }
}

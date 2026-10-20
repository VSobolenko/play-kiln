package com.gexetr.plinthproductphotos.domain.provenance

import com.gexetr.plinthproductphotos.data.api.API_BASE_URL
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.ApiTransport
import com.gexetr.plinthproductphotos.data.api.OkHttpApiTransport
import com.gexetr.plinthproductphotos.data.api.TransportRequest
import com.gexetr.plinthproductphotos.data.api.TransportResponse
import com.gexetr.plinthproductphotos.data.api.parseApiError
import java.io.IOException
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

@Serializable
enum class ReportCategory {
    OFFENSIVE,
    @SerialName("WRONG_PRODUCT") WRONG_PRODUCT,
    UNSAFE,
    RIGHTS,
    OTHER,
}

@Serializable
enum class ReportStatus {
    OPEN,
    REVIEWED,
    ACTIONED,
    DISMISSED,
}

@Serializable
data class ReportResponse(
    val id: Long,
    val status: ReportStatus,
)

@Serializable
data class ReportStatusResponse(
    val id: Long,
    val status: ReportStatus,
    val category: ReportCategory,
    val createdAt: Long,
    val caseStatus: String,
)

@Serializable
private data class ReportRequest(
    val jobId: String,
    val category: ReportCategory,
    val note: String? = null,
)

class ReportApiClient(
    baseUrl: String = API_BASE_URL,
    private val transport: ApiTransport = OkHttpApiTransport(),
) {
    private val reportsUrl = "${baseUrl.trimEnd('/')}/reports"
    private val confirmed = mutableMapOf<ConfirmedReportKey, ReportResponse>()

    @Synchronized
    fun submit(
        jobId: String,
        category: ReportCategory,
        note: String?,
        deviceToken: String,
    ): ApiResult<ReportResponse> {
        require(jobId.isNotBlank())
        require(deviceToken.isNotBlank())
        require(note == null || note.length <= MAX_NOTE_LENGTH)

        val request = ReportRequest(
            jobId = jobId,
            category = category,
            note = note?.trim()?.takeIf(String::isNotEmpty),
        )
        val key = ConfirmedReportKey(request, tokenDigest(deviceToken))
        confirmed[key]?.let { return ApiResult.Success(it) }
        val response = try {
            transport.execute(
                TransportRequest(
                    method = "POST",
                    url = reportsUrl,
                    headers = mapOf(
                        "Authorization" to "Bearer $deviceToken",
                        "Content-Type" to "application/json",
                    ),
                    body = ApiJson.encodeToString(request).toByteArray(Charsets.UTF_8),
                ),
            )
        } catch (_: IOException) {
            return ApiResult.Failure(ApiFailure.Network(null))
        }

        if (response.statusCode != REPORT_CREATED) return parseApiError(response.statusCode, response.body)
        val result = decodeResponse(response)
        if (result is ApiResult.Success) confirmed[key] = result.value
        return result
    }

    fun getStatus(reportId: Long, deviceToken: String): ApiResult<ReportStatusResponse> {
        require(reportId > 0)
        require(deviceToken.isNotBlank())
        val response = try {
            transport.execute(
                TransportRequest(
                    method = "GET",
                    url = "$reportsUrl/${reportId}",
                    headers = mapOf("Authorization" to "Bearer $deviceToken"),
                ),
            )
        } catch (_: IOException) {
            return ApiResult.Failure(ApiFailure.Network(null))
        }
        if (response.statusCode != STATUS_OK) return parseApiError(response.statusCode, response.body)
        return try {
            ApiResult.Success(
                ApiJson.decodeFromString<ReportStatusResponse>(response.body.toString(Charsets.UTF_8)),
            )
        } catch (_: SerializationException) {
            invalidStatusResponse()
        } catch (_: IllegalArgumentException) {
            invalidStatusResponse()
        }
    }

    private fun decodeResponse(response: TransportResponse): ApiResult<ReportResponse> = try {
        ApiResult.Success(ApiJson.decodeFromString<ReportResponse>(response.body.toString(Charsets.UTF_8)))
    } catch (_: SerializationException) {
        invalidResponse()
    } catch (_: IllegalArgumentException) {
        invalidResponse()
    }

    private fun tokenDigest(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun invalidResponse(): ApiResult.Failure = ApiResult.Failure(
        ApiFailure.Unknown(
            code = "INVALID_REPORT_RESPONSE",
            message = "The server response did not match the expected format.",
            retryAfterSec = null,
            details = JsonObject(emptyMap()),
        ),
    )

    private fun invalidStatusResponse(): ApiResult.Failure = ApiResult.Failure(
        ApiFailure.Unknown(
            code = "INVALID_REPORT_STATUS_RESPONSE",
            message = "The server response did not match the expected format.",
            retryAfterSec = null,
            details = JsonObject(emptyMap()),
        ),
    )

    companion object {
        const val MAX_NOTE_LENGTH = 500
        private const val REPORT_CREATED = 201
        private const val STATUS_OK = 200
    }

    private data class ConfirmedReportKey(
        val request: ReportRequest,
        val tokenDigest: String,
    )
}

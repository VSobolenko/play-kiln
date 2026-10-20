package com.gexetr.plinthproductphotos.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

val ApiJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class IntegrityChallengeRequest(
    val installId: String,
    val appVersion: String,
    val locale: String,
)

@Serializable
data class IntegrityChallengeResponse(
    val challengeId: String,
    val challenge: String,
    val expiresAt: Long,
)

@Serializable
data class SessionRequest(
    val installId: String,
    val appVersion: String,
    val locale: String,
    val integrityToken: String,
    val challengeId: String,
    val challenge: String,
)

@Serializable
data class SessionResponse(
    val deviceToken: String,
    val walletId: String,
    val balance: Int,
    val starterGranted: Boolean,
    val config: ApiConfigResponse,
)

@Serializable
data class ApiConfigResponse(
    val ops: Map<String, OperationConfig>,
    val starterCredits: Int,
    val killSwitch: Boolean,
    val minAppVersion: String,
    val dailyCapReached: Boolean,
    val serviceState: ServiceState = ServiceState.OK,
    val notice: ServiceNotice? = null,
)

@Serializable
enum class ServiceState {
    @SerialName("ok") OK,
    @SerialName("maintenance") MAINTENANCE,
    @SerialName("closed") CLOSED,
    @SerialName("update_required") UPDATE_REQUIRED,
}

@Serializable
data class ServiceNotice(
    val code: String,
    val messageByLocale: Map<String, String> = emptyMap(),
    val dismissible: Boolean = false,
)

@Serializable
data class OperationConfig(
    val credits: Int,
    val enabled: Boolean,
)

@Serializable
data class WalletResponse(
    val balance: Int,
    val reserved: Int,
    val refundDebt: Int,
    val purchaseHold: Int,
    val entries: List<WalletEntry>,
    val unlocked: Boolean,
)

@Serializable
data class WalletEntry(
    val kind: String,
    val delta: Int,
    val createdAt: Long,
)

@Serializable
data class UploadResponse(
    val uploadId: String,
    val expiresAt: Long,
)

data class RecognitionRequest(
    val jpegBytes: ByteArray,
)

@Serializable
data class RecognitionResponse(
    val category: RecognitionCategory,
    val confidence: Double,
    val attributes: List<String>,
    val visibleText: List<String>,
    val scaleHint: RecognitionScaleHint,
    val sku: String? = null,
) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0)
        require(attributes.size <= 8)
        require(visibleText.size <= 6)
    }
}

@Serializable
enum class RecognitionCategory {
    @SerialName("clothing_top") CLOTHING_TOP,
    @SerialName("clothing_bottom") CLOTHING_BOTTOM,
    @SerialName("dress") DRESS,
    @SerialName("outerwear") OUTERWEAR,
    @SerialName("shoes") SHOES,
    @SerialName("bag") BAG,
    @SerialName("accessory") ACCESSORY,
    @SerialName("jewelry") JEWELRY,
    @SerialName("watch") WATCH,
    @SerialName("cosmetics") COSMETICS,
    @SerialName("skincare") SKINCARE,
    @SerialName("packaged_food") PACKAGED_FOOD,
    @SerialName("beverage") BEVERAGE,
    @SerialName("electronics_small") ELECTRONICS_SMALL,
    @SerialName("home_decor") HOME_DECOR,
    @SerialName("kitchenware") KITCHENWARE,
    @SerialName("toy") TOY,
    @SerialName("book") BOOK,
    @SerialName("furniture_small") FURNITURE_SMALL,
    @SerialName("tool") TOOL,
    @SerialName("other") OTHER,
}

@Serializable
data class RecognitionScaleHint(
    val kind: RecognitionScaleKind,
    val valueMm: Double? = null,
) {
    init {
        require(valueMm == null || (valueMm.isFinite() && valueMm > 0.0))
    }
}

@Serializable
enum class RecognitionScaleKind {
    @SerialName("none") NONE,
    @SerialName("ruler") RULER,
    @SerialName("coin") COIN,
    @SerialName("hand") HAND,
    @SerialName("label") LABEL,
}

@Serializable
data class JobRequest(
    val clientRequestId: String,
    val op: OperationId,
    val uploadIds: List<String>,
    val params: JsonObject,
    val estimateCredits: Int,
    val marketplaceId: String? = null,
)

@Serializable
enum class OperationId {
    @SerialName("O-01") RECOGNISE,
    @SerialName("O-02") CUTOUT,
    @SerialName("O-07") GENERATED_SCENE,
    @SerialName("O-08") RELIGHT,
    @SerialName("O-09") EXPAND,
    @SerialName("O-10") RECOLOUR,
    @SerialName("O-11") UPSCALE,
    @SerialName("O-12") ERASE,
    @SerialName("O-13") GHOST_MANNEQUIN,
    @SerialName("O-14") VIRTUAL_MODEL,
    @SerialName("O-15") LISTING_TEXT,
}

@Serializable
data class JobAccepted(
    val jobId: String,
    val reserved: Int,
    val state: JobState,
)

@Serializable
enum class JobState {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}

@Serializable
data class JobStatus(
    val state: JobState,
    val progress: Int? = null,
    val result: JobResult? = null,
    val charged: Int,
    val reserved: Int = 0,
    val released: Int = 0,
    val error: ApiJobError? = null,
) {
    val notCharged: Boolean
        get() = (state == JobState.FAILED || state == JobState.CANCELLED) &&
            reserved > 0 && released == reserved && charged == 0
}

@Serializable
data class JobResult(
    val resultToken: String,
    val expiresAt: Long,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val sha256: String,
    val provenance: String,
    val guard: JsonObject? = null,
)

@Serializable
data class ApiJobError(
    val code: String,
    val message: String,
) {
    val typedCode: ApiJobErrorCode?
        get() = ApiJobErrorCode.entries.firstOrNull { it.wireCode == code }
}

enum class ApiJobErrorCode(val wireCode: String) {
    PROVIDER_FAILED("PROVIDER_FAILED"),
    GUARD_REJECTED("GUARD_REJECTED"),
    MODERATION_BLOCKED("MODERATION_BLOCKED"),
    OPERATION_DISABLED("OPERATION_DISABLED"),
}

@Serializable
data class DeliveryReceipt(
    val deliveryAttemptId: String,
    val bytes: Long,
    val sha256: String,
)

@Serializable
data class DeliveryReceiptResponse(
    val charged: Int,
    val state: String,
)

@Serializable
data class CancelResponse(
    val state: JobState,
    val released: Int,
)

@Serializable
data class ApiErrorEnvelope(
    val code: String,
    val message: String,
    val retryAfterSec: Int? = null,
    val details: JsonObject = JsonObject(emptyMap()),
)

@Serializable
private data class ApiErrorWrapper(
    val error: ApiErrorEnvelope,
)

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(
        val failure: ApiFailure,
        val envelope: ApiErrorEnvelope? = null,
    ) : ApiResult<Nothing>
}

sealed interface ApiFailure {
    data object Unauthorized : ApiFailure
    data object DeviceBlocked : ApiFailure
    data object InsufficientCredits : ApiFailure
    data object PriceChanged : ApiFailure
    data object PurchaseInvalid : ApiFailure
    data object PurchasePending : ApiFailure
    data object RateLimited : ApiFailure
    data object CapReached : ApiFailure
    data object KillSwitch : ApiFailure
    data object ServiceMaintenance : ApiFailure
    data object ServiceClosed : ApiFailure
    data object TooLarge : ApiFailure
    data object UnsupportedMedia : ApiFailure
    data object VersionTooOld : ApiFailure
    data object ProviderFailed : ApiFailure
    data object GuardRejected : ApiFailure
    data object ModerationBlocked : ApiFailure
    data object NotConfigured : ApiFailure
    data object OperationDisabled : ApiFailure
    data object ResultExpired : ApiFailure
    data object ResultIntegrityFailed : ApiFailure
    data object DeliveryReceiptMismatch : ApiFailure
    data class Network(
        val message: String?,
        val category: String? = null,
    ) : ApiFailure
    data class Unknown(
        val code: String,
        val message: String,
        val retryAfterSec: Int?,
        val details: JsonObject,
    ) : ApiFailure
}

fun mapApiError(error: ApiErrorEnvelope): ApiFailure = when (error.code) {
    "UNAUTHORIZED" -> ApiFailure.Unauthorized
    "DEVICE_BLOCKED" -> ApiFailure.DeviceBlocked
    "INSUFFICIENT_CREDITS" -> ApiFailure.InsufficientCredits
    "PRICE_CHANGED" -> ApiFailure.PriceChanged
    "PURCHASE_INVALID" -> ApiFailure.PurchaseInvalid
    "PURCHASE_PENDING" -> ApiFailure.PurchasePending
    "RATE_LIMITED" -> ApiFailure.RateLimited
    "CAP_REACHED" -> ApiFailure.CapReached
    "KILL_SWITCH" -> ApiFailure.KillSwitch
    "SERVICE_MAINTENANCE" -> ApiFailure.ServiceMaintenance
    "SERVICE_CLOSED" -> ApiFailure.ServiceClosed
    "UPDATE_REQUIRED" -> ApiFailure.VersionTooOld
    "TOO_LARGE" -> ApiFailure.TooLarge
    "UNSUPPORTED_MEDIA" -> ApiFailure.UnsupportedMedia
    "VERSION_TOO_OLD" -> ApiFailure.VersionTooOld
    "PROVIDER_FAILED" -> ApiFailure.ProviderFailed
    "GUARD_REJECTED" -> ApiFailure.GuardRejected
    "MODERATION_BLOCKED" -> ApiFailure.ModerationBlocked
    "NOT_CONFIGURED" -> ApiFailure.NotConfigured
    "OPERATION_DISABLED" -> ApiFailure.OperationDisabled
    "RESULT_EXPIRED" -> ApiFailure.ResultExpired
    "RESULT_INTEGRITY_FAILED" -> ApiFailure.ResultIntegrityFailed
    "DELIVERY_RECEIPT_MISMATCH" -> ApiFailure.DeliveryReceiptMismatch
    else -> ApiFailure.Unknown(error.code, error.message, error.retryAfterSec, error.details)
}

internal fun parseApiError(statusCode: Int, body: ByteArray): ApiResult.Failure {
    val envelope = try {
        ApiJson.decodeFromString<ApiErrorWrapper>(body.toString(Charsets.UTF_8)).error
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
    return if (envelope == null) {
        ApiResult.Failure(
            ApiFailure.Unknown(
                code = "HTTP_$statusCode",
                message = "The server returned an unreadable error response.",
                retryAfterSec = null,
                details = JsonObject(emptyMap()),
            ),
        )
    } else {
        ApiResult.Failure(mapApiError(envelope), envelope)
    }
}

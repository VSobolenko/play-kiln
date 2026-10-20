package com.gexetr.plinthproductphotos.data.wallet

import com.gexetr.plinthproductphotos.data.api.API_BASE_URL
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.TransportRequest
import com.gexetr.plinthproductphotos.data.api.TransportResponse
import com.gexetr.plinthproductphotos.data.api.OkHttpApiTransport
import com.gexetr.plinthproductphotos.data.api.ApiTransport
import com.gexetr.plinthproductphotos.data.api.parseApiError
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

interface WalletPurchaseApi {
    fun createIntent(
        productId: String,
        deviceToken: String,
    ): ApiResult<PurchaseIntentResponse>

    fun confirmPurchase(
        intentId: String,
        productId: String,
        purchaseToken: String,
        deviceToken: String,
    ): ApiResult<PurchaseConfirmationResponse>

    fun recoverWallet(code: String): ApiResult<WalletRecoveryResponse>
}

@Serializable
data class PurchaseIntentResponse(
    val intentId: String,
    val obfuscatedAccountId: String,
    val obfuscatedProfileId: String,
    val expiresAt: Long,
)

@Serializable
data class PurchaseConfirmationResponse(
    val credited: Int,
    val balance: Int,
    val state: String,
    val orderId: String? = null,
) {
    val terminal: Boolean
        get() = state == STATE_CREDITED || state == STATE_ENTITLED

    companion object {
        const val STATE_CREDITED = "CREDITED"
        const val STATE_ENTITLED = "ENTITLED"
    }
}

@Serializable
data class WalletRecoveryResponse(
    val deviceToken: String,
    val walletId: String,
    val balance: Int,
)

@Serializable
private data class PurchaseIntentRequest(val productId: String)

@Serializable
private data class PurchaseConfirmationRequest(
    val intentId: String,
    val productId: String,
    val purchaseToken: String,
)

@Serializable
private data class WalletRecoveryRequest(val code: String)

class HttpWalletPurchaseApi(
    baseUrl: String = API_BASE_URL,
    private val transport: ApiTransport = OkHttpApiTransport(),
) : WalletPurchaseApi {
    private val root = baseUrl.trimEnd('/')

    override fun createIntent(
        productId: String,
        deviceToken: String,
    ): ApiResult<PurchaseIntentResponse> = request(
        method = "POST",
        path = "/purchases/intents",
        body = ApiJson.encodeToString(PurchaseIntentRequest(productId)),
        deviceToken = deviceToken,
        successCode = 201,
    )

    override fun confirmPurchase(
        intentId: String,
        productId: String,
        purchaseToken: String,
        deviceToken: String,
    ): ApiResult<PurchaseConfirmationResponse> = request(
        method = "POST",
        path = "/purchases/confirm",
        body = ApiJson.encodeToString(PurchaseConfirmationRequest(intentId, productId, purchaseToken)),
        deviceToken = deviceToken,
        successCode = 200,
    )

    override fun recoverWallet(code: String): ApiResult<WalletRecoveryResponse> = request(
        method = "POST",
        path = "/wallet/recover",
        body = ApiJson.encodeToString(WalletRecoveryRequest(code)),
        deviceToken = null,
        successCode = 200,
    )

    private inline fun <reified T> request(
        method: String,
        path: String,
        body: String,
        deviceToken: String?,
        successCode: Int,
    ): ApiResult<T> {
        val response = try {
            transport.execute(
                TransportRequest(
                    method = method,
                    url = "$root$path",
                    headers = buildMap {
                        put("Content-Type", "application/json")
                        deviceToken?.let { put("Authorization", "Bearer $it") }
                    },
                    body = body.toByteArray(Charsets.UTF_8),
                ),
            )
        } catch (_: IOException) {
            return ApiResult.Failure(ApiFailure.Network(null))
        }
        if (response.statusCode != successCode) {
            return parseApiError(response.statusCode, response.body)
        }
        return decode(response)
    }

    private inline fun <reified T> decode(response: TransportResponse): ApiResult<T> =
        try {
            ApiResult.Success(ApiJson.decodeFromString(response.body.toString(Charsets.UTF_8)))
        } catch (_: SerializationException) {
            ApiResult.Failure(
                ApiFailure.Unknown(
                    code = "INVALID_SUCCESS_BODY",
                    message = "The server response did not match the expected format.",
                    retryAfterSec = null,
                    details = kotlinx.serialization.json.JsonObject(emptyMap()),
                ),
            )
        } catch (_: IllegalArgumentException) {
            ApiResult.Failure(
                ApiFailure.Unknown(
                    code = "INVALID_SUCCESS_BODY",
                    message = "The server response did not match the expected format.",
                    retryAfterSec = null,
                    details = kotlinx.serialization.json.JsonObject(emptyMap()),
                ),
            )
        }
}

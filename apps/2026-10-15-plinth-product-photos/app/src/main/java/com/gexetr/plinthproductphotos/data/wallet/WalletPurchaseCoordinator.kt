package com.gexetr.plinthproductphotos.data.wallet

import android.content.Context
import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.BillingController
import com.gexetr.capability.billing.ConsumablePack
import com.gexetr.capability.billing.Fulfillment
import com.gexetr.capability.billing.ProductId
import com.gexetr.capability.billing.PurchaseAuthority
import com.gexetr.capability.billing.PurchaseIntent
import com.gexetr.capability.billing.PurchaseResolution
import com.gexetr.capability.core.PlayKilnResult
import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.WalletResponse
import com.gexetr.plinthproductphotos.data.outbox.PurchaseOutbox
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WalletProductIds(
    val starter: ProductId,
    val studio: ProductId,
    val pro: ProductId,
    val unlock: ProductId,
) {
    fun billingCatalog(): BillingCatalog = BillingCatalog(
        inAppProducts = listOf(unlock),
        consumables = listOf(
            ConsumablePack(starter, grants = 90, unit = "credits", fulfillment = Fulfillment.SERVER_COST),
            ConsumablePack(studio, grants = 230, unit = "credits", fulfillment = Fulfillment.SERVER_COST),
            ConsumablePack(pro, grants = 600, unit = "credits", fulfillment = Fulfillment.SERVER_COST),
        ),
    )

    fun contains(product: ProductId): Boolean =
        product == starter || product == studio || product == pro || product == unlock
}

class WalletPurchaseAuthority(
    context: Context,
    private val sessionStore: WalletSessionStore,
    private val api: WalletPurchaseApi = HttpWalletPurchaseApi(),
    private val outbox: PurchaseOutbox = PurchaseOutbox(context),
) : PurchaseAuthority {
    override suspend fun confirmPurchase(
        purchaseToken: String,
        product: ProductId,
    ): PlayKilnResult<Unit> = PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

    override suspend fun confirmPurchase(
        purchaseToken: String,
        product: ProductId,
        intentId: String,
    ): PlayKilnResult<Unit> {
        val session = sessionStore.current()
            ?: return PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
        outbox.enqueue(product.value, intentId, purchaseToken)
        val response = withContext(Dispatchers.IO) {
            api.confirmPurchase(
                intentId = intentId,
                productId = product.value,
                purchaseToken = purchaseToken,
                deviceToken = session.deviceToken,
            )
        }
        return when (response) {
            is ApiResult.Success -> {
                if (!response.value.terminal) {
                    PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_AVAILABLE)
                } else {
                    outbox.markConfirmed(sha256Hex(purchaseToken))
                    PlayKilnResult.Success(Unit)
                }
            }
            is ApiResult.Failure -> {
                if (response.failure == ApiFailure.PurchaseInvalid) {
                    outbox.markRejected(sha256Hex(purchaseToken))
                }
                PlayKilnResult.Failure(response.failure.toBillingReason())
            }
        }
    }

    override suspend fun confirmSpend(unit: String, amount: Long): PlayKilnResult<Unit> =
        PlayKilnResult.Failure(PlayKilnResult.Reason.NOT_CONFIGURED)

}

data class WalletSummary(
    val balance: Int,
    val reserved: Int,
    val refundDebt: Int,
    val purchaseHold: Int,
    val entries: List<com.gexetr.plinthproductphotos.data.api.WalletEntry>,
)

class WalletPurchaseCoordinator(
    private val controller: BillingController,
    private val sessionStore: WalletSessionStore,
    private val products: WalletProductIds,
    private val purchaseApi: WalletPurchaseApi = HttpWalletPurchaseApi(),
    private val apiClient: ApiClient = ApiClient(),
) {
    suspend fun connect(): PlayKilnResult<Unit> = controller.connect(products.billingCatalog())

    suspend fun localizedPrices(): Map<String, String> = buildMap {
        listOf(products.starter, products.studio, products.pro, products.unlock).forEach { product ->
            controller.formattedPrice(product)?.let { put(product.value, it) }
        }
    }

    suspend fun purchase(product: ProductId): WalletPurchaseResult {
        require(products.contains(product)) { "Product is not part of the wallet catalogue" }
        val session = sessionStore.current()
            ?: return WalletPurchaseResult.Failed(ApiFailure.Unauthorized)
        val intentResult = withContext(Dispatchers.IO) {
            purchaseApi.createIntent(product.value, session.deviceToken)
        }
        val intentResponse = when (intentResult) {
            is ApiResult.Success -> intentResult.value
            is ApiResult.Failure -> return WalletPurchaseResult.Failed(intentResult.failure)
        }
        val intent = PurchaseIntent(
            intentId = intentResponse.intentId,
            obfuscatedAccountId = intentResponse.obfuscatedAccountId,
            obfuscatedProfileId = intentResponse.obfuscatedProfileId,
        )
        return when (val result = controller.purchase(product, intent)) {
            is PlayKilnResult.Failure -> WalletPurchaseResult.Failed(null)
            is PlayKilnResult.Success -> WalletPurchaseResult.from(result.value)
        }
    }

    suspend fun refreshWallet(): ApiResult<WalletSummary> {
        val session = sessionStore.current()
            ?: return ApiResult.Failure(ApiFailure.Unauthorized)
        val result = withContext(Dispatchers.IO) { apiClient.getWallet(session.deviceToken) }
        return when (result) {
            is ApiResult.Success -> {
                sessionStore.updateServerBalance(result.value.balance)
                ApiResult.Success(result.value.toSummary())
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun recoverWallet(code: String): ApiResult<WalletSummary> {
        val recovered = withContext(Dispatchers.IO) { purchaseApi.recoverWallet(code) }
        when (recovered) {
            is ApiResult.Success -> recovered.value.also {
                sessionStore.saveSession(it.deviceToken, it.walletId, it.balance)
            }
            is ApiResult.Failure -> return recovered
        }
        return refreshWallet()
    }
}

sealed interface WalletPurchaseResult {
    data object Confirming : WalletPurchaseResult
    data object Pending : WalletPurchaseResult
    data object Purchased : WalletPurchaseResult
    data class Failed(val reason: ApiFailure?) : WalletPurchaseResult

    companion object {
        fun from(resolution: PurchaseResolution): WalletPurchaseResult = when (resolution.state) {
            com.gexetr.capability.billing.PurchaseResolutionState.CONSUMABLE_CREDITED,
            com.gexetr.capability.billing.PurchaseResolutionState.ENTITLEMENT_GRANTED -> Purchased
            com.gexetr.capability.billing.PurchaseResolutionState.AWAITING_PAYMENT -> Pending
            com.gexetr.capability.billing.PurchaseResolutionState.PURCHASE_OBSERVED -> Confirming
        }
    }
}

private fun ApiFailure.toBillingReason(): PlayKilnResult.Reason = when (this) {
    ApiFailure.Unauthorized,
    ApiFailure.DeviceBlocked -> PlayKilnResult.Reason.NOT_AVAILABLE
    is ApiFailure.Network,
    ApiFailure.RateLimited,
    ApiFailure.CapReached,
    ApiFailure.KillSwitch,
    ApiFailure.ServiceMaintenance,
    ApiFailure.ServiceClosed,
    ApiFailure.PurchasePending,
    ApiFailure.NotConfigured,
    is ApiFailure.Unknown -> PlayKilnResult.Reason.NETWORK
    ApiFailure.PurchaseInvalid -> PlayKilnResult.Reason.NOT_AVAILABLE
    ApiFailure.InsufficientCredits,
    ApiFailure.PriceChanged,
    ApiFailure.TooLarge,
    ApiFailure.UnsupportedMedia,
    ApiFailure.VersionTooOld,
    ApiFailure.ProviderFailed,
    ApiFailure.GuardRejected,
    ApiFailure.ModerationBlocked,
    ApiFailure.OperationDisabled,
    ApiFailure.ResultExpired,
    ApiFailure.ResultIntegrityFailed,
    ApiFailure.DeliveryReceiptMismatch -> PlayKilnResult.Reason.UNKNOWN
}

private fun WalletResponse.toSummary() = WalletSummary(
    balance = balance,
    reserved = reserved,
    refundDebt = refundDebt,
    purchaseHold = purchaseHold,
    entries = entries,
)

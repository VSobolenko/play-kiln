package com.gexetr.plinthproductphotos.data.outbox

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gexetr.plinthproductphotos.data.api.ApiFailure
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore
import com.gexetr.plinthproductphotos.data.wallet.HttpWalletPurchaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseFulfilmentWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val outbox = PurchaseOutbox(applicationContext)
        val purchases = outbox.pending()
        if (purchases.isEmpty()) return Result.success()

        val session = WalletSessionStore(applicationContext).current() ?: return Result.retry()
        val api = HttpWalletPurchaseApi()

        for (purchase in purchases) {
            when (
                val result = withContext(Dispatchers.IO) {
                    api.confirmPurchase(
                        intentId = purchase.intentId,
                        productId = purchase.productId,
                        purchaseToken = purchase.purchaseToken,
                        deviceToken = session.deviceToken,
                    )
                }
            ) {
                is ApiResult.Success -> {
                    if (result.value.terminal) {
                        outbox.markConfirmed(purchase.tokenHash)
                    } else if (result.value.state == STATE_PENDING) {
                        return Result.retry()
                    } else {
                        outbox.markRejected(purchase.tokenHash)
                    }
                }
                is ApiResult.Failure -> {
                    if (result.failure == ApiFailure.PurchaseInvalid) {
                        outbox.markRejected(purchase.tokenHash)
                    } else {
                        return Result.retry()
                    }
                }
            }
        }
        return Result.success()
    }

    private companion object {
        const val STATE_PENDING = "PENDING"
    }
}

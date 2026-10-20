package com.gexetr.plinthproductphotos.data.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gexetr.plinthproductphotos.data.session.PendingCutoutStore
import com.gexetr.plinthproductphotos.data.session.WalletSessionStore

class DeliveryReceiptWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val attemptId = inputData.getString(KEY_ATTEMPT_ID) ?: return Result.failure()
        val bytes = inputData.getLong(KEY_BYTES, -1L)
        val sha256 = inputData.getString(KEY_SHA256) ?: return Result.failure()
        if (bytes < 0L) return Result.failure()

        val session = WalletSessionStore(applicationContext).current() ?: return Result.retry()
        return when (
            val response = ApiClient().submitDeliveryReceipt(
                jobId,
                DeliveryReceipt(attemptId, bytes, sha256),
                session.deviceToken,
            )
        ) {
            is ApiResult.Success -> {
                if (response.value.state != "SETTLED") return Result.failure()
                PendingCutoutStore(applicationContext).clear()
                Result.success()
            }
            is ApiResult.Failure -> when (response.failure) {
                is ApiFailure.Network -> Result.retry()
                ApiFailure.Unauthorized -> Result.failure()
                ApiFailure.DeliveryReceiptMismatch -> Result.failure()
                ApiFailure.InsufficientCredits -> Result.failure()
                ApiFailure.DeviceBlocked -> Result.failure()
                ApiFailure.ResultExpired -> Result.failure()
                ApiFailure.ResultIntegrityFailed -> Result.failure()
                ApiFailure.CapReached -> Result.retry()
                ApiFailure.KillSwitch -> Result.retry()
                ApiFailure.ServiceMaintenance -> Result.retry()
                ApiFailure.ServiceClosed -> Result.failure()
                ApiFailure.NotConfigured -> Result.retry()
                ApiFailure.OperationDisabled -> Result.retry()
                ApiFailure.PriceChanged -> Result.failure()
                ApiFailure.PurchaseInvalid -> Result.failure()
                ApiFailure.PurchasePending -> Result.retry()
                ApiFailure.RateLimited -> Result.retry()
                ApiFailure.TooLarge -> Result.failure()
                ApiFailure.UnsupportedMedia -> Result.failure()
                ApiFailure.VersionTooOld -> Result.failure()
                ApiFailure.ProviderFailed -> Result.failure()
                ApiFailure.GuardRejected -> Result.failure()
                ApiFailure.ModerationBlocked -> Result.failure()
                is ApiFailure.Unknown -> Result.retry()
            }
        }
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_ATTEMPT_ID = "delivery_attempt_id"
        const val KEY_BYTES = "result_bytes"
        const val KEY_SHA256 = "result_sha256"
    }
}

package com.gexetr.plinthproductphotos.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.first

private val Context.cutoutQueueDataStore by preferencesDataStore(name = "cutout_queue")

data class PendingCutout(
    val clientRequestId: String,
    val jobId: String?,
    val sourcePath: String?,
    val receipt: PendingDeliveryReceipt?,
)

data class PendingDeliveryReceipt(
    val jobId: String,
    val deliveryAttemptId: String,
    val bytes: Long,
    val sha256: String,
    val maskPath: String,
)

class PendingCutoutStore(context: Context) {
    private val dataStore = context.applicationContext.cutoutQueueDataStore

    suspend fun getOrCreate(sourcePath: String): PendingCutout {
        var pending: PendingCutout? = null
        dataStore.edit { preferences ->
            val requestId = preferences[REQUEST_ID] ?: UUID.randomUUID().toString().also {
                preferences[REQUEST_ID] = it
                preferences[SOURCE_PATH] = sourcePath
            }
            pending = readPending(requestId, preferences)
        }
        return requireNotNull(pending)
    }

    suspend fun current(): PendingCutout? {
        val preferences = dataStore.data.first()
        val requestId = preferences[REQUEST_ID] ?: return null
        return readPending(requestId, preferences)
    }

    suspend fun saveJobId(jobId: String) {
        dataStore.edit { preferences -> preferences[JOB_ID] = jobId }
    }

    suspend fun saveReceipt(receipt: PendingDeliveryReceipt) {
        dataStore.edit { preferences ->
            preferences[RECEIPT_JOB_ID] = receipt.jobId
            preferences[RECEIPT_ATTEMPT_ID] = receipt.deliveryAttemptId
            preferences[RECEIPT_BYTES] = receipt.bytes
            preferences[RECEIPT_SHA256] = receipt.sha256
            preferences[RECEIPT_MASK_PATH] = receipt.maskPath
        }
    }

    suspend fun clearReceipt() {
        dataStore.edit { preferences ->
            preferences.remove(RECEIPT_JOB_ID)
            preferences.remove(RECEIPT_ATTEMPT_ID)
            preferences.remove(RECEIPT_BYTES)
            preferences.remove(RECEIPT_SHA256)
            preferences.remove(RECEIPT_MASK_PATH)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(REQUEST_ID)
            preferences.remove(JOB_ID)
            preferences.remove(SOURCE_PATH)
            preferences.remove(RECEIPT_JOB_ID)
            preferences.remove(RECEIPT_ATTEMPT_ID)
            preferences.remove(RECEIPT_BYTES)
            preferences.remove(RECEIPT_SHA256)
            preferences.remove(RECEIPT_MASK_PATH)
        }
    }

    private fun readPending(
        requestId: String,
        preferences: androidx.datastore.preferences.core.Preferences,
    ): PendingCutout {
        val receiptJobId = preferences[RECEIPT_JOB_ID]
        val receiptAttemptId = preferences[RECEIPT_ATTEMPT_ID]
        val receiptBytes = preferences[RECEIPT_BYTES]
        val receiptSha256 = preferences[RECEIPT_SHA256]
        val maskPath = preferences[RECEIPT_MASK_PATH]
        val receipt = if (receiptJobId != null && receiptAttemptId != null &&
            receiptBytes != null && receiptSha256 != null && maskPath != null
        ) {
            PendingDeliveryReceipt(receiptJobId, receiptAttemptId, receiptBytes, receiptSha256, maskPath)
        } else {
            null
        }
        return PendingCutout(
            clientRequestId = requestId,
            jobId = preferences[JOB_ID],
            sourcePath = preferences[SOURCE_PATH],
            receipt = receipt,
        )
    }

    private companion object {
        val REQUEST_ID = stringPreferencesKey("pending_client_request_id")
        val JOB_ID = stringPreferencesKey("pending_job_id")
        val SOURCE_PATH = stringPreferencesKey("pending_source_path")
        val RECEIPT_JOB_ID = stringPreferencesKey("pending_receipt_job_id")
        val RECEIPT_ATTEMPT_ID = stringPreferencesKey("pending_receipt_attempt_id")
        val RECEIPT_BYTES = androidx.datastore.preferences.core.longPreferencesKey("pending_receipt_bytes")
        val RECEIPT_SHA256 = stringPreferencesKey("pending_receipt_sha256")
        val RECEIPT_MASK_PATH = stringPreferencesKey("pending_receipt_mask_path")
    }
}

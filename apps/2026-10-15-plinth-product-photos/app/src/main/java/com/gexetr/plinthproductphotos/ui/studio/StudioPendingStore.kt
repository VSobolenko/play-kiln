package com.gexetr.plinthproductphotos.ui.studio

import android.content.Context
import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.DeliveryReceipt
import com.gexetr.plinthproductphotos.data.api.OperationId
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class StudioPending(
    val requestId: String,
    val photoId: String,
    val walletId: String,
    val operation: OperationId,
    val credits: Int,
    val params: JsonObject,
    val uploadId: String? = null,
    val jobId: String? = null,
    val resultPath: String? = null,
    val receipt: DeliveryReceipt? = null,
    val charged: Int? = null,
    val terminal: Boolean = false,
    val applied: Boolean = false,
) {
    val canReplace: Boolean
        get() = terminal || (charged == credits && resultPath != null && receipt != null)
}

internal class StudioPendingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "studio_pending", Context.MODE_PRIVATE,
    )

    fun current(): StudioPending? = preferences.getString("request", null)?.let {
        ApiJson.decodeFromString<StudioPending>(it)
    }

    fun save(pending: StudioPending) {
        if (!preferences.edit().putString("request", ApiJson.encodeToString(pending)).commit()) {
            throw IOException("STUDIO_PENDING_WRITE_FAILED")
        }
    }

    fun clear() {
        if (!preferences.edit().remove("request").commit()) {
            throw IOException("STUDIO_PENDING_CLEAR_FAILED")
        }
    }
}

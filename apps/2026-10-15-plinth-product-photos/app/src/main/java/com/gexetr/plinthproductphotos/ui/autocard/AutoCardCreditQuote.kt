package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.data.api.ApiConfigResponse
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.data.api.ApiJson
import kotlinx.serialization.json.jsonPrimitive

data class AutoCardCreditQuote(
    val credits: Int,
    val balance: Int,
    val available: Boolean,
) {
    val canRecognize: Boolean get() = available && balance >= credits
}

internal fun autoCardCreditQuote(config: ApiConfigResponse, balance: Int): AutoCardCreditQuote? {
    val operationId = ApiJson.encodeToJsonElement(OperationId.serializer(), OperationId.RECOGNISE)
        .jsonPrimitive.content
    val operation = config.ops[operationId] ?: return null
    if (operation.credits < 0 || balance < 0) return null
    return AutoCardCreditQuote(
        credits = operation.credits,
        balance = balance,
        available = operation.enabled && !config.killSwitch && !config.dailyCapReached,
    )
}

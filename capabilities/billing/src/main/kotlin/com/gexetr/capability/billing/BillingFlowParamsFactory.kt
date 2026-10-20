package com.gexetr.capability.billing

import com.android.billingclient.api.BillingFlowParams

internal object BillingFlowParamsFactory {
    fun create(
        productDetails: List<BillingFlowParams.ProductDetailsParams>,
        intent: PurchaseIntent?,
    ): BillingFlowParams {
        val builder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetails)

        if (intent != null) {
            builder
                .setObfuscatedAccountId(intent.obfuscatedAccountId)
                .setObfuscatedProfileId(intent.obfuscatedProfileId)
        }

        return builder.build()
    }
}

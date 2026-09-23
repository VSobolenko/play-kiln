package com.gexetr.capability.billing

/**
 * A Play Billing product identifier, e.g. `premium` or `coins_100`.
 *
 * Play enforces its own rules at upload time, but a typo found there costs a release
 * cycle, so [validate] catches it locally instead.
 */
@JvmInline
value class ProductId(val value: String) {
    /** Non-null message when invalid, `null` when valid. */
    fun validate(): String? =
        if (PRODUCT_ID_PATTERN.matches(value)) {
            null
        } else {
            "product ID '$value' must match ${PRODUCT_ID_PATTERN.pattern}"
        }

    /** Entitlement key for this product. */
    fun key(): ProductKey = ProductKey(value)

    companion object {
        /**
         * Play product IDs start with a lowercase letter or digit and may then contain
         * lowercase letters, digits, underscores, and dots. Uppercase, dashes, spaces, and
         * leading separators are rejected.
         */
        private val PRODUCT_ID_PATTERN = Regex("^[a-z0-9][a-z0-9._]*$")
    }
}

/** Key into an [Entitlements] set; separate from [ProductId] because entitlements outlive products. */
@JvmInline
value class ProductKey(val value: String)

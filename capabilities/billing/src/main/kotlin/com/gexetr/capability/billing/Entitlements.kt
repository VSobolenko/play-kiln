package com.gexetr.capability.billing

/**
 * Lifecycle of a subscription as Play reports it.
 *
 * `GRACE_PERIOD` counts as active on purpose: Play deliberately keeps serving a paying
 * user while a payment failure is retried, and revoking access during that window creates
 * a support ticket and an unfair refund request.
 */
enum class SubscriptionStatus {
    ACTIVE,
    GRACE_PERIOD,
    ON_HOLD,
    PAUSED,
    CANCELED,
    EXPIRED,

    ;

    val isActive: Boolean
        get() = this == ACTIVE || this == GRACE_PERIOD
}

/**
 * What the user currently owns — the single question the UI actually cares about.
 *
 * Kept as plain data so it can be persisted, compared in tests, and reasoned about
 * without touching the billing SDK.
 */
data class Entitlements(
    val owned: Set<ProductKey> = emptySet(),
    val subscriptions: Map<ProductKey, SubscriptionStatus> = emptyMap(),
) {
    fun owns(key: ProductKey): Boolean = key in owned || hasActiveSubscription(key)

    fun ownKeys(): Set<ProductKey> = owned

    fun hasActiveSubscription(key: ProductKey): Boolean = subscriptions[key]?.isActive == true

    fun hasAnyActiveSubscription(): Boolean = subscriptions.values.any { it.isActive }

    /** Returns entitlements with [key] granted, replacing an existing subscription status. */
    fun granting(key: ProductKey, asSubscription: Boolean): Entitlements =
        if (asSubscription) {
            copy(subscriptions = subscriptions + (key to SubscriptionStatus.ACTIVE))
        } else {
            copy(owned = owned + key)
        }

    companion object {
        val EMPTY = Entitlements()
    }
}

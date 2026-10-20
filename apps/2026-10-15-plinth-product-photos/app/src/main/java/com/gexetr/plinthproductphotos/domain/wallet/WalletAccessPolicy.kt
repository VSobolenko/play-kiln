package com.gexetr.plinthproductphotos.domain.wallet

import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePreset

object WalletAccessPolicy {
    const val FREE_BATCH_LIMIT = 5
    const val UNLOCKED_BATCH_LIMIT = 50

    enum class ExportAccess {
        UNLOCKED,
        WATERMARK_REQUIRED,
        BLOCKED_BY_PRESET,
    }

    data class BatchAccess(
        val allowed: Boolean,
        val maximumItems: Int?,
    )

    fun isUnlocked(serverConfirmedUnlock: Boolean): Boolean = serverConfirmedUnlock

    fun batchAccess(itemCount: Int, serverConfirmedUnlock: Boolean): BatchAccess {
        require(itemCount >= 0) { "Batch item count cannot be negative" }
        val allowed = serverConfirmedUnlock || itemCount <= FREE_BATCH_LIMIT
        return BatchAccess(
            allowed = allowed,
            maximumItems = FREE_BATCH_LIMIT.takeUnless { serverConfirmedUnlock },
        )
    }

    fun exportAccess(
        serverConfirmedUnlock: Boolean,
        preset: MarketplacePreset?,
    ): ExportAccess = when {
        serverConfirmedUnlock -> ExportAccess.UNLOCKED
        preset?.editPolicy == EditPolicy.ORIGINAL_ONLY -> ExportAccess.BLOCKED_BY_PRESET
        else -> ExportAccess.WATERMARK_REQUIRED
    }

    fun watermarkRequired(access: ExportAccess): Boolean = access == ExportAccess.WATERMARK_REQUIRED

    /** How many photos one selection may hold: the picker limit and the selection-state cap. */
    fun selectionLimit(serverConfirmedUnlock: Boolean): Int =
        if (isUnlocked(serverConfirmedUnlock)) UNLOCKED_BATCH_LIMIT else FREE_BATCH_LIMIT

    /** The picker and its result clamp must use the same server-confirmed entitlement. */
    fun pickerSelectionLimit(serverConfirmedUnlock: Boolean): Int =
        selectionLimit(serverConfirmedUnlock)

    fun <T> clampSelection(items: List<T>, serverConfirmedUnlock: Boolean): List<T> =
        items.take(selectionLimit(serverConfirmedUnlock))

    fun selectionExceedsLimit(itemCount: Int, serverConfirmedUnlock: Boolean): Boolean =
        itemCount > selectionLimit(serverConfirmedUnlock)

    fun paidPathAllowed(preset: MarketplacePreset?): Boolean =
        preset?.editPolicy != EditPolicy.ORIGINAL_ONLY

    fun canSpendCredits(balance: Int, refundDebt: Int, cost: Int): Boolean {
        require(balance >= 0) { "Credit balance cannot be negative" }
        require(refundDebt >= 0) { "Refund debt cannot be negative" }
        require(cost >= 0) { "Credit cost cannot be negative" }
        return cost == 0 || (refundDebt == 0 && balance >= cost)
    }
}

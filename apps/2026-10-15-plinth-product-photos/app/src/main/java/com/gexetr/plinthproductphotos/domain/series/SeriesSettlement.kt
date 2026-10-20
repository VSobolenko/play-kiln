package com.gexetr.plinthproductphotos.domain.series

enum class SeriesVariantStatus {
    PENDING,
    DELIVERED,
    FAILED,
    EXPIRED,
}

data class SeriesVariantSettlement(
    val variantId: String,
    val status: SeriesVariantStatus,
    val settledCredits: Int,
    val releasedCredits: Int,
)

data class SeriesSettlement private constructor(
    val seriesId: String,
    val reservedCredits: Int,
    val perVariantCredits: Int,
    val variants: List<SeriesVariantSettlement>,
    val isTerminal: Boolean,
) {
    val settledCredits: Int
        get() = variants.sumOf(SeriesVariantSettlement::settledCredits)

    val releasedCredits: Int
        get() = variants.sumOf(SeriesVariantSettlement::releasedCredits)

    val reservedRemainderCredits: Int
        get() = reservedCredits - settledCredits - releasedCredits

    val deliveredVariants: Int
        get() = variants.count { it.status == SeriesVariantStatus.DELIVERED }

    fun delivered(variantId: String): SeriesSettlement = updatePending(variantId) {
        it.copy(status = SeriesVariantStatus.DELIVERED, settledCredits = perVariantCredits)
    }

    fun failed(variantId: String): SeriesSettlement = updatePending(variantId) {
        it.copy(status = SeriesVariantStatus.FAILED, releasedCredits = perVariantCredits)
    }

    fun expired(variantId: String): SeriesSettlement = updatePending(variantId) {
        it.copy(status = SeriesVariantStatus.EXPIRED, releasedCredits = perVariantCredits)
    }

    fun finish(): SeriesSettlement {
        if (isTerminal) return this
        val finished = copy(
            variants = variants.map { variant ->
                if (variant.status == SeriesVariantStatus.PENDING) {
                    variant.copy(status = SeriesVariantStatus.EXPIRED, releasedCredits = perVariantCredits)
                } else {
                    variant
                }
            },
            isTerminal = true,
        )
        finished.requireLedgerInvariant()
        return finished
    }

    private fun updatePending(
        variantId: String,
        update: (SeriesVariantSettlement) -> SeriesVariantSettlement,
    ): SeriesSettlement {
        check(!isTerminal) { "A terminal series cannot change" }
        var found = false
        val updated = copy(
            variants = variants.map { variant ->
                if (variant.variantId != variantId) {
                    variant
                } else {
                    check(variant.status == SeriesVariantStatus.PENDING) {
                        "Variant $variantId already has a terminal result"
                    }
                    found = true
                    update(variant)
                }
            },
        )
        require(found) { "Unknown series variant $variantId" }
        updated.requireLedgerInvariant()
        return updated
    }

    private fun requireLedgerInvariant() {
        require(reservedCredits == settledCredits + releasedCredits + reservedRemainderCredits) {
            "Series movements must reconcile to the parent reservation"
        }
        require(reservedRemainderCredits >= 0) { "Series movements exceed the parent reservation" }
        require(variants.all { variant ->
            when (variant.status) {
                SeriesVariantStatus.PENDING ->
                    variant.settledCredits == 0 && variant.releasedCredits == 0
                SeriesVariantStatus.DELIVERED ->
                    variant.settledCredits == perVariantCredits && variant.releasedCredits == 0
                SeriesVariantStatus.FAILED,
                SeriesVariantStatus.EXPIRED ->
                    variant.settledCredits == 0 && variant.releasedCredits == perVariantCredits
            }
        }) { "Each child movement must match its terminal result" }
        if (isTerminal) {
            require(variants.none { it.status == SeriesVariantStatus.PENDING })
            require(reservedCredits == settledCredits + releasedCredits)
        }
    }

    companion object {
        fun fromReservation(
            seriesId: String,
            reservedCredits: Int,
            perVariantCredits: Int,
            variantIds: List<String>,
        ): SeriesSettlement {
            require(seriesId.isNotBlank()) { "Series id is required" }
            require(perVariantCredits > 0) { "Per-variant price must be positive" }
            require(variantIds.isNotEmpty()) { "A series must contain at least one variant" }
            require(variantIds.all(String::isNotBlank) && variantIds.distinct().size == variantIds.size) {
                "Variant ids must be non-empty and unique"
            }
            require(reservedCredits == Math.multiplyExact(perVariantCredits, variantIds.size)) {
                "Parent reservation must equal the declared per-variant amount"
            }
            return SeriesSettlement(
                seriesId = seriesId,
                reservedCredits = reservedCredits,
                perVariantCredits = perVariantCredits,
                variants = variantIds.map { id ->
                    SeriesVariantSettlement(id, SeriesVariantStatus.PENDING, 0, 0)
                },
                isTerminal = false,
            )
        }
    }
}

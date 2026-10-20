package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.model.SlotRole
import java.util.Locale

object FileNaming {
    fun packName(marketplaceId: String): String = "${safeSegment(marketplaceId)}.zip"
    fun bundleName(): String = "plinth-marketplace-packs.zip"

    fun name(sku: String, role: SlotRole, photoOrder: Int, format: String): String {
        require(photoOrder > 0) { "Photo order must be positive" }
        val extension = when (format.lowercase(Locale.ROOT)) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            else -> throw IllegalArgumentException("Unsupported export format: $format")
        }
        return "${safeSegment(sku)}_${role.name.lowercase(Locale.ROOT)}_$photoOrder.$extension"
    }

    internal fun uniqueSkus(itemIdsAndSkus: List<Pair<String, String>>): Map<String, String> {
        val assigned = linkedMapOf<String, String>()
        val used = mutableSetOf<String>()
        itemIdsAndSkus.forEach { (itemId, originalSku) ->
            if (itemId in assigned) {
                require(assigned.getValue(itemId) == safeSegment(originalSku)) {
                    "An item cannot have multiple SKUs in one export"
                }
            } else {
                val base = safeSegment(originalSku)
                var candidate = base
                var suffix = 2
                while (!used.add(candidate.lowercase(Locale.ROOT))) {
                    candidate = "$base-$suffix"
                    suffix++
                }
                assigned[itemId] = candidate
            }
        }
        return assigned
    }

    private fun safeSegment(value: String): String {
        val normalized = value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('.', '-', '_')
            .ifBlank { "item" }
        val reserved = normalized.substringBefore('.').uppercase(Locale.ROOT)
        return if (reserved in WINDOWS_RESERVED_NAMES) "item-$normalized" else normalized
    }

    private val WINDOWS_RESERVED_NAMES = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        (1..9).forEach { add("COM$it"); add("LPT$it") }
    }
}

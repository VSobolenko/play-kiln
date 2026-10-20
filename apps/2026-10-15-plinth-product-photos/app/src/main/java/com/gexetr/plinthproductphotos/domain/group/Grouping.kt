package com.gexetr.plinthproductphotos.domain.group

import com.gexetr.plinthproductphotos.domain.model.SlotRole

data class GroupPhoto(
    val id: String,
    val sku: String,
    val photoOrder: Int = 0,
    val role: SlotRole = SlotRole.MAIN,
)

data class PhotoGroup(val sku: String, val photos: List<GroupPhoto>) {
    fun exportablePhotos(): List<GroupPhoto> = photos.filter { it.role != SlotRole.REFERENCE }
}

object Grouping {
    private val photoOrdering = compareBy<GroupPhoto> { it.role != SlotRole.MAIN }
        .thenBy(GroupPhoto::photoOrder)

    fun regroup(photos: List<GroupPhoto>, pattern: SkuPattern): List<PhotoGroup> {
        require(photos.map(GroupPhoto::id).distinct().size == photos.size) { "Photo ids must be unique" }
        val parsed = photos.map { photo ->
            photo to (pattern.parse(photo.sku)
                ?: throw IllegalArgumentException("SKU '${photo.sku}' does not match the seller pattern"))
        }
        return parsed.groupBy { (photo, _) -> photo.sku.lowercase() }
            .map { (_, entries) ->
                PhotoGroup(
                    sku = entries.first().first.sku,
                    photos = entries.map { it.first }.sortedWith(photoOrdering),
                )
            }
            .sortedWith(
                compareBy<PhotoGroup> { pattern.parse(it.sku)?.base?.lowercase().orEmpty() }
                    .thenBy { pattern.parse(it.sku)?.number ?: Int.MAX_VALUE }
                    .thenBy(PhotoGroup::sku),
            )
    }

    fun movePhoto(groups: List<PhotoGroup>, photoId: String, targetSku: String, pattern: SkuPattern): List<PhotoGroup> {
        require(pattern.parse(targetSku) != null) { "Target SKU does not match the seller pattern" }
        val moving = groups.flatMap(PhotoGroup::photos).singleOrNull { it.id == photoId }
            ?: throw IllegalArgumentException("Photo '$photoId' must belong to exactly one group")
        val updated = groups.mapNotNull { group ->
            val remaining = group.photos.filterNot { it.id == photoId }
            if (remaining.isEmpty()) null else group.copy(photos = remaining)
        }.toMutableList()
        val targetIndex = updated.indexOfFirst { it.sku == targetSku }
        if (targetIndex >= 0) {
            val target = updated[targetIndex]
            updated[targetIndex] = target.copy(photos = (target.photos + moving).sortedWith(photoOrdering))
        } else {
            updated += PhotoGroup(targetSku, listOf(moving.copy(sku = targetSku)))
        }
        return updated.sortedBy(PhotoGroup::sku)
    }
}

data class RecognitionSuggestion(
    val value: String,
    val kind: Kind,
    val confirmed: Boolean = false,
) {
    enum class Kind { SCALE, SKU }

    fun confirm(): RecognitionSuggestion = copy(confirmed = true)
}

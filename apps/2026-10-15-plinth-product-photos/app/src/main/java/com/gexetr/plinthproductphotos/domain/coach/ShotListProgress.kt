package com.gexetr.plinthproductphotos.domain.coach

data class CoachShot(
    val id: String,
    val isReference: Boolean = false,
)

data class ShotListProgress(
    val category: String,
    val shots: List<CoachShot>,
    val completedIds: Set<String> = emptySet(),
    val skippedIds: Set<String> = emptySet(),
) {
    val completedCount: Int
        get() = completedIds.size

    val currentShot: CoachShot?
        get() = shots.firstOrNull { it.id !in completedIds && it.id !in skippedIds }

    val completedShots: List<CoachShot>
        get() = shots.filter { it.id in completedIds }

    val skippedShots: List<CoachShot>
        get() = shots.filter { it.id in skippedIds }

    fun completeCurrent(): ShotListProgress {
        val shot = currentShot ?: return this
        return copy(completedIds = completedIds + shot.id)
    }

    fun skipCurrent(): ShotListProgress {
        val shot = currentShot ?: return this
        return copy(skippedIds = skippedIds + shot.id)
    }

    companion object {
        fun forCategory(category: String): ShotListProgress {
            val normalized = category.trim().lowercase()
            val shots = when (normalized) {
                "clothing", "apparel", "garment" -> listOf(
                    CoachShot("front"),
                    CoachShot("back"),
                    CoachShot("label", isReference = true),
                    CoachShot("flaw"),
                    CoachShot("measurement", isReference = true),
                )
                "shoes", "footwear" -> listOf(
                    CoachShot("front"),
                    CoachShot("side"),
                    CoachShot("sole"),
                    CoachShot("label", isReference = true),
                    CoachShot("flaw"),
                )
                "jewellery", "jewelry" -> listOf(
                    CoachShot("front"),
                    CoachShot("side"),
                    CoachShot("clasp"),
                    CoachShot("mark", isReference = true),
                    CoachShot("scale", isReference = true),
                )
                "electronics" -> listOf(
                    CoachShot("front"),
                    CoachShot("back"),
                    CoachShot("ports"),
                    CoachShot("label", isReference = true),
                    CoachShot("flaw"),
                )
                else -> listOf(
                    CoachShot("front"),
                    CoachShot("back"),
                    CoachShot("label", isReference = true),
                    CoachShot("flaw"),
                    CoachShot("measurement", isReference = true),
                )
            }
            return ShotListProgress(category = normalized, shots = shots)
        }
    }
}

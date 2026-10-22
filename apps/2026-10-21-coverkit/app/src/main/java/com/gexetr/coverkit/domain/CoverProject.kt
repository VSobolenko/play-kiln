package com.gexetr.coverkit.domain

import kotlinx.serialization.Serializable

@Serializable
data class CoverProject(
    val id: String,
    val destinationId: String,
    val name: String,
    val layers: List<Layer> = emptyList(),
    val updatedAtMs: Long = 0L,
    /** Index into [BackdropStyle] — keeps thumbs and editor source in sync. */
    val backdropStyle: Int = 0,
) {
    /** Convenience for editor mutations that only replace the layer tree. */
    fun withLayers(layers: List<Layer>, nowMs: Long = System.currentTimeMillis()): CoverProject =
        copy(layers = layers, updatedAtMs = nowMs)
}

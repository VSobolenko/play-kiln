package com.gexetr.plinthproductphotos.domain.model

data class BackgroundSpec(
    val kind: Kind,
    val colorArgb: Int?,
    val color2Argb: Int?,
    val angleDeg: Int?,
    val sceneId: String?,
    val sceneAssetPath: String?,
    val prompt: String?,
    val seed: Long?,
) {
    enum class Kind {
        SOLID,
        GRADIENT,
        PROCEDURAL_SCENE,
        GENERATED_SCENE,
    }
}

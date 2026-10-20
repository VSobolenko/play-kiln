package com.gexetr.plinthproductphotos.domain.model

data class ShadowSpec(
    val kind: Kind = Kind.CONTACT,
    val opacity: Float = 0.35f,
    val blurPct: Float = 0.02f,
    val offsetPct: Float = 0.010f,
) {
    enum class Kind {
        NONE,
        CONTACT,
        DROP,
    }
}

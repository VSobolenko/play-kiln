package com.gexetr.plinthproductphotos.domain.model

data class Look(
    val id: String,
    val name: String,
    val background: BackgroundSpec,
    val shadow: ShadowSpec,
    val marginPct: Float = 0.06f,
    val fillTarget: Float = 0.80f,
    val slideStyleId: String? = null,
    val createdAt: Long,
)

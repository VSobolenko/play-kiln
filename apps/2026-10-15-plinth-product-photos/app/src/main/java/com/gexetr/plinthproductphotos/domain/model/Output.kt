package com.gexetr.plinthproductphotos.domain.model

data class Output(
    val id: String,
    val itemId: String,
    val path: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val format: String,
    val provenance: Provenance,
    val operations: List<String>,
    val marketplaceId: String?,
    val role: SlotRole,
    val createdAt: Long,
)

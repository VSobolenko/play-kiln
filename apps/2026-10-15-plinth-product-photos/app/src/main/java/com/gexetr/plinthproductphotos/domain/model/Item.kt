package com.gexetr.plinthproductphotos.domain.model

data class Item(
    val id: String,
    val projectId: String,
    val sourcePath: String,
    val sku: String?,
    val groupId: String?,
    val role: SlotRole,
    val category: String?,
    val categoryConfirmed: Boolean,
    val attributesJson: String?,
    val scaleHintJson: String?,
    val maskPath: String?,
    val maskRevision: Int,
    val state: ItemState,
    val lastError: String?,
)

package com.gexetr.plinthproductphotos.domain.library

data class LibrarySearchEntry(
    val projectId: String,
    val text: String,
    val skus: List<String>,
    val category: String?,
    val marketplaceId: String? = null,
    val updatedAt: Long = 0L,
)

data class LibraryFilters(
    val category: String? = null,
    val marketplaceId: String? = null,
    val updatedAfter: Long? = null,
    val updatedBefore: Long? = null,
)

fun librarySearchQuery(searchQuery: String): String? = searchQuery.trim().takeIf(String::isNotEmpty)

fun searchLibrary(
    projects: List<LibrarySearchEntry>,
    query: String,
    filters: LibraryFilters = LibraryFilters(),
): List<LibrarySearchEntry> {
    val needle = query.trim()
    return projects.filter { project ->
        (needle.isEmpty() || project.text.contains(needle, ignoreCase = true) ||
            project.skus.any { it.contains(needle, ignoreCase = true) } ||
            project.category?.contains(needle, ignoreCase = true) == true) &&
            (filters.category == null || project.category.equals(filters.category, ignoreCase = true)) &&
            (filters.marketplaceId == null || project.marketplaceId == filters.marketplaceId) &&
            (filters.updatedAfter == null || project.updatedAt >= filters.updatedAfter) &&
            (filters.updatedBefore == null || project.updatedAt <= filters.updatedBefore)
    }
}

data class ItemOperationResult(
    val itemId: String,
    val succeeded: Boolean,
    val error: String? = null,
)

object BulkOperationExecutor {
    fun changeMarketplace(
        selectedItemIds: List<String>,
        marketplaceId: String,
        changeOne: (itemId: String, marketplaceId: String) -> Unit,
    ): List<ItemOperationResult> {
        require(selectedItemIds.distinct().size == selectedItemIds.size) { "Selected item ids must be unique" }
        return selectedItemIds.map { itemId ->
            try {
                changeOne(itemId, marketplaceId)
                ItemOperationResult(itemId, succeeded = true)
            } catch (failure: Exception) {
                ItemOperationResult(
                    itemId = itemId,
                    succeeded = false,
                    error = failure.message ?: failure::class.simpleName ?: "Operation failed",
                )
            }
        }
    }
}

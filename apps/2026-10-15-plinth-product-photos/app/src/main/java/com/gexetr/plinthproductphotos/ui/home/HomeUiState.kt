package com.gexetr.plinthproductphotos.ui.home

import androidx.compose.runtime.Immutable
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto

@Immutable
data class HomeUiState(
    val recentPhotos: List<ProjectPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val photoCount: Int = recentPhotos.size,
)

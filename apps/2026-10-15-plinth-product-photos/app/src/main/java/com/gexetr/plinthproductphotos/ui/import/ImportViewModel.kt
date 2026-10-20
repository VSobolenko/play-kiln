package com.gexetr.plinthproductphotos.ui.importscreen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.data.photoimport.PhotoImporter
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import org.json.JSONException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportFailureKind {
    NOT_IMAGE,
    STORAGE,
    UNAVAILABLE,
    UNKNOWN,
}

data class ImportFailure(val selectionNumber: Int, val kind: ImportFailureKind)

data class ImportUiState(
    val isImporting: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val importedPhotos: List<ProjectPhoto> = emptyList(),
    val failures: List<ImportFailure> = emptyList(),
    val selectedUris: List<Uri> = emptyList(),
)

class ImportViewModel(
    context: Context,
) : ViewModel() {
    private val projects = ProjectRepository(context.applicationContext)
    private val importer = PhotoImporter(context.applicationContext, projects)
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isImporting) return
        _uiState.value = ImportUiState(isImporting = true, totalCount = uris.size, selectedUris = uris)
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEachIndexed { index, uri ->
                try {
                    val photo = importer.importPhoto(uri)
                    _uiState.update { state ->
                        state.copy(
                            completedCount = index + 1,
                            importedPhotos = state.importedPhotos + photo,
                        )
                    }
                } catch (_: IOException) {
                    _uiState.update { state ->
                        state.copy(
                            completedCount = index + 1,
                            failures = state.failures + ImportFailure(index + 1, ImportFailureKind.STORAGE),
                        )
                    }
                } catch (_: SecurityException) {
                    _uiState.update { state ->
                        state.copy(
                            completedCount = index + 1,
                            failures = state.failures + ImportFailure(index + 1, ImportFailureKind.UNAVAILABLE),
                        )
                    }
                } catch (_: IllegalArgumentException) {
                    _uiState.update { state ->
                        state.copy(
                            completedCount = index + 1,
                            failures = state.failures + ImportFailure(index + 1, ImportFailureKind.NOT_IMAGE),
                        )
                    }
                } catch (_: JSONException) {
                    _uiState.update { state ->
                        state.copy(
                            completedCount = index + 1,
                            failures = state.failures + ImportFailure(index + 1, ImportFailureKind.UNKNOWN),
                        )
                    }
                }
            }
            _uiState.update { it.copy(isImporting = false) }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ImportViewModel(applicationContext) as T
            }
        }
    }
}

package com.gexetr.plinthproductphotos.ui.editor

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.toArgb
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImage
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectOutputRepository
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.data.DataStoreSettingsStore
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import com.gexetr.plinthproductphotos.ui.theme.PlinthProductPalette
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val isLoading: Boolean = true,
    val photoName: String = "",
    val settings: CompositionSettings? = null,
    val original: Bitmap? = null,
    val composition: Bitmap? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val saveState: EditorSaveState = EditorSaveState.IDLE,
    val previewComposition: Bitmap? = null,
    val error: Boolean = false,
)

enum class EditorSaveState {
    IDLE,
    SAVING,
    SAVED,
    FAILED,
}

class EditorViewModel(context: Context) : ViewModel() {
    private val settingsStore = DataStoreSettingsStore(context.applicationContext)
    private val loader = ProjectImageLoader(ProjectRepository(context.applicationContext))
    private val outputRepository = ProjectOutputRepository(context.applicationContext)
    private var image: ProjectImage? = null
    private var loadedMaskAlpha: ByteArray? = null
    private var session: LocalCompositionSession? = null
    private var persistedSettings: CompositionSettings? = null
    private var previewJob: Job? = null
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun load(photoId: String, initialMaskAlpha: ByteArray? = null) {
        val maskCopy = initialMaskAlpha?.copyOf()
        if (image?.photo?.id == photoId && loadedMaskAlpha.contentEqualsNullable(maskCopy)) return
        previewJob?.cancel()
        persistedSettings = null
        _uiState.value = EditorUiState()
        viewModelScope.launch {
            try {
                val (loaded, savedSettings) = withContext(Dispatchers.IO) {
                    loader.load(photoId) to outputRepository.latestAutoCardSettings(photoId)
                }
                val mask = maskCopy?.let {
                    require(it.size == loaded.source.pixels.size) { "Mask must match the source photo" }
                    Mask(it)
                } ?: loaded.mask
                val initialSettings = savedSettings ?: run {
                    val defaults = settingsStore.settings.first().studioDefaults
                    val background = when (defaults.background) {
                        1 -> PlinthProductPalette.Graphite
                        2 -> PlinthProductPalette.Travertine
                        else -> PlinthProductPalette.Paper
                    }
                    defaults.toCompositionSettings(background.toArgb())
                }
                image = loaded
                loadedMaskAlpha = maskCopy
                val initial = LocalCompositionSession(
                    source = loaded.source,
                    mask = mask,
                    canvas = Canvas(CARD_SIZE, CARD_SIZE),
                    initial = initialSettings,
                )
                session = initial
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        photoName = loaded.photo.name,
                        settings = initial.settings,
                        original = loaded.source.toBitmap(),
                        composition = initial.composition.image.toBitmap(),
                        hasUnsavedChanges = true,
                    )
                }
            } catch (_: IOException) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun updateSettings(settings: CompositionSettings, commandKey: String) {
        if (_uiState.value.saveState == EditorSaveState.SAVING) return
        val current = session ?: return
        clearPreview()
        try {
            current.apply(settings, commandKey)
            publish(current)
        } catch (_: IllegalArgumentException) {
            _uiState.update { it.copy(error = true) }
        }
    }

    fun setShadow(kind: ShadowSpec.Kind) {
        val settings = _uiState.value.settings ?: return
        updateSettings(settings.copy(shadow = settings.shadow.copy(kind = kind)), SHADOW_COMMAND)
    }

    fun undo() {
        if (_uiState.value.saveState == EditorSaveState.SAVING) return
        clearPreview()
        val current = session ?: return
        current.undo()
        publish(current)
    }

    fun redo() {
        if (_uiState.value.saveState == EditorSaveState.SAVING) return
        clearPreview()
        val current = session ?: return
        current.redo()
        publish(current)
    }

    fun previewSettings(settings: CompositionSettings) {
        if (_uiState.value.saveState == EditorSaveState.SAVING) return
        val current = session ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                delay(40)
                val preview = withContext(Dispatchers.Default) {
                    current.preview(settings).image.toBitmap()
                }
                _uiState.update { it.copy(previewComposition = preview, error = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(error = true) }
            }
        }
    }

    fun clearPreview() {
        previewJob?.cancel()
        previewJob = null
        _uiState.update { it.copy(previewComposition = null) }
    }

    fun save() {
        val current = session
        val itemId = image?.photo?.id
        if (current == null || itemId == null) {
            _uiState.update { it.copy(saveState = EditorSaveState.FAILED) }
            return
        }
        if (_uiState.value.saveState == EditorSaveState.SAVING) return
        val pixels = current.composition.image
        val settings = current.settings
        _uiState.update { it.copy(saveState = EditorSaveState.SAVING) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    outputRepository.saveAutoCard(itemId, pixels, settings)
                }
                persistedSettings = settings
                _uiState.update {
                    it.copy(
                        hasUnsavedChanges = false,
                        saveState = EditorSaveState.SAVED,
                        error = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saveState = EditorSaveState.FAILED) }
            }
        }
    }

    private fun publish(current: LocalCompositionSession) {
        _uiState.update {
            val dirty = persistedSettings?.let { saved -> current.settings != saved } ?: true
            it.copy(
                settings = current.settings,
                composition = current.composition.image.toBitmap(),
                canUndo = current.canUndo,
                canRedo = current.canRedo,
                hasUnsavedChanges = dirty,
                saveState = if (dirty) EditorSaveState.IDLE
                else if (persistedSettings != null) EditorSaveState.SAVED
                else EditorSaveState.IDLE,
                previewComposition = null,
                error = false,
            )
        }
    }

    companion object {
        private const val CARD_SIZE = 1200
        private const val SHADOW_COMMAND = "shadow"

        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditorViewModel(applicationContext) as T
            }
        }

        private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
            if (this == null) other == null else other != null && contentEquals(other)
    }
}

package com.gexetr.plinthproductphotos.ui.card

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImage
import com.gexetr.plinthproductphotos.data.photoimport.ProjectImageLoader
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.export.Watermark
import com.gexetr.plinthproductphotos.domain.export.WatermarkGate
import com.gexetr.plinthproductphotos.domain.model.Canvas
import com.gexetr.plinthproductphotos.ui.wallet.ServerUnlock
import com.gexetr.plinthproductphotos.ui.editor.CompositionSettings
import com.gexetr.plinthproductphotos.ui.editor.LocalCompositionSession
import com.gexetr.plinthproductphotos.ui.editor.solidBackground
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CardExportState { IDLE, SAVING, SAVED, ERROR }

data class CardUiState(
    val isLoading: Boolean = true,
    val isComposing: Boolean = false,
    val photoName: String = "",
    val composition: Bitmap? = null,
    val error: Boolean = false,
    val original: Bitmap? = null,
    val exportState: CardExportState = CardExportState.IDLE,
    val savedDestination: String? = null,
)

class CardViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val loader = ProjectImageLoader(ProjectRepository(appContext))
    private var image: ProjectImage? = null
    private var renderJob: Job? = null
    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    fun load(photoId: String, backgroundArgb: Int) {
        if (image?.photo?.id == photoId) return
        renderJob?.cancel()
        image = null
        _uiState.value = CardUiState()
        renderJob = viewModelScope.launch {
            try {
                val (loaded, original) = withContext(Dispatchers.IO) {
                    val loaded = loader.load(photoId)
                    loaded to loaded.source.toBitmap()
                }
                image = loaded
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isComposing = true,
                        photoName = loaded.photo.name,
                        original = original,
                    )
                }
                val rendered = renderCard(loaded, backgroundArgb)
                _uiState.update { it.copy(isComposing = false, composition = rendered) }
            } catch (_: IOException) {
                _uiState.update { it.copy(isLoading = false, isComposing = false, error = true) }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(isLoading = false, isComposing = false, error = true) }
            }
        }
    }

    fun makeCard(backgroundArgb: Int) {
        val currentState = _uiState.value
        if (currentState.isComposing || currentState.composition != null ||
            currentState.exportState == CardExportState.SAVING
        ) return
        val loaded = image ?: return
        _uiState.update {
            it.copy(
                isComposing = true,
                error = false,
                exportState = CardExportState.IDLE,
                savedDestination = null,
            )
        }
        renderJob = viewModelScope.launch {
            try {
                val rendered = renderCard(loaded, backgroundArgb)
                _uiState.update { it.copy(isComposing = false, composition = rendered) }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(isComposing = false, error = true) }
            }
        }
    }

    fun exportCard(uri: Uri) {
        val snapshot = _uiState.value
        if (snapshot.isComposing || snapshot.exportState == CardExportState.SAVING) return
        val bitmap = snapshot.composition ?: return
        _uiState.update { it.copy(exportState = CardExportState.SAVING) }
        viewModelScope.launch {
            try {
                val unlocked = ServerUnlock.fetch(appContext)
                val destination = withContext(Dispatchers.IO) {
                    val output = if (WatermarkGate.required(unlocked)) watermarked(bitmap) else bitmap
                    val stream = appContext.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Could not open selected save location")
                    stream.use {
                        if (!output.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                            throw IOException("Could not encode product card PNG")
                        }
                    }
                    ensureActive()
                    selectedDestination(uri)
                }
                _uiState.update {
                    if (it.composition === bitmap) {
                        it.copy(exportState = CardExportState.SAVED, savedDestination = destination)
                    } else {
                        it
                    }
                }
            } catch (_: IOException) {
                _uiState.update {
                    if (it.composition === bitmap) it.copy(exportState = CardExportState.ERROR) else it
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    if (it.composition === bitmap) it.copy(exportState = CardExportState.ERROR) else it
                }
            }
        }
    }

    private fun watermarked(source: Bitmap): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IOException("Could not prepare the product card for export")
        val pixels = IntArray(copy.width * copy.height)
        copy.getPixels(pixels, 0, copy.width, 0, 0, copy.width, copy.height)
        Watermark.applyArgb(pixels, copy.width, copy.height)
        copy.setPixels(pixels, 0, copy.width, 0, 0, copy.width, copy.height)
        return copy
    }

    private suspend fun renderCard(loaded: ProjectImage, backgroundArgb: Int): Bitmap =
        withContext(Dispatchers.Default) {
            LocalCompositionSession(
                source = loaded.source,
                mask = loaded.mask,
                canvas = Canvas(CARD_SIZE, CARD_SIZE),
                initial = CompositionSettings(background = solidBackground(backgroundArgb)),
            ).composition.image.toBitmap()
        }

    private fun selectedDestination(uri: Uri): String {
        if (uri.authority?.contains("downloads", ignoreCase = true) == true) {
            return appContext.getString(R.string.card_destination_downloads)
        }
        if (!DocumentsContract.isDocumentUri(appContext, uri)) return uri.toString()

        val documentId = DocumentsContract.getDocumentId(uri)
        val providerPath = documentId.substringAfter(':', documentId)
        val directory = providerPath.substringBeforeLast('/', missingDelimiterValue = "")
        if (directory.isNotBlank()) return directory
        return when {
            documentId.startsWith("primary:") ->
                appContext.getString(R.string.card_destination_internal_storage)
            documentId.contains(':') ->
                appContext.getString(R.string.card_destination_external_storage)
            else -> uri.toString()
        }
    }

    companion object {
        private const val CARD_SIZE = 1200

        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CardViewModel(applicationContext) as T
            }
        }
    }
}

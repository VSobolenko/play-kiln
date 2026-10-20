package com.gexetr.plinthproductphotos.ui.autocard

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer

/** Keeps previews in memory across configuration changes, not process death. */
class AutoCardResultsViewModel : ViewModel() {
    private val results = mutableStateMapOf<String, PixelBuffer>()

    operator fun get(photoId: String): PixelBuffer? = results[photoId]

    operator fun set(photoId: String, pixels: PixelBuffer) {
        results[photoId] = pixels
    }
}

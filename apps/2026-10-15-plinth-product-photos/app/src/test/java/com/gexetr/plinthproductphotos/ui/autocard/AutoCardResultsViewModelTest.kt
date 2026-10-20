package com.gexetr.plinthproductphotos.ui.autocard

import androidx.lifecycle.ViewModelStore
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AutoCardResultsViewModelTest {
    @Test
    fun resultSurvivesHolderBeingReadAgainFromRetainedStore() {
        val store = ViewModelStore()
        try {
            val holder = AutoCardResultsViewModel()
            store.put("auto-card", holder)
            val pixels = PixelBuffer.solid(2, 2, PixelBuffer.rgba(80, 90, 100))
            holder["photo-1"] = pixels

            val reread = store["auto-card"] as AutoCardResultsViewModel

            assertSame(pixels, reread["photo-1"])
            assertSame(pixels, reread["photo-1"])
        } finally {
            store.clear()
        }
    }

    @Test
    fun resultsArePhotoSpecificAndMissingResultIsNull() {
        val holder = AutoCardResultsViewModel()
        val first = PixelBuffer.solid(1, 1, PixelBuffer.rgba(80, 90, 100))
        val second = PixelBuffer.solid(1, 1, PixelBuffer.rgba(10, 20, 30))
        holder["photo-1"] = first
        holder["photo-2"] = second

        assertSame(first, holder["photo-1"])
        assertSame(second, holder["photo-2"])
        assertNull(holder["missing"])
        assertNull(AutoCardResultsViewModel()["photo-1"])
    }
}

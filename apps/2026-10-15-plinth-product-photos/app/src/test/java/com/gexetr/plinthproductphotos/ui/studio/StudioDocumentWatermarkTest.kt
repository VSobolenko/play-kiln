package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.domain.export.Watermark
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDocumentWatermarkTest {
    @Test
    fun freeStudioDocumentSaveRequiresTheExistingWatermark() {
        assertTrue(studioDocumentWatermarkRequired(serverConfirmedUnlock = false))

        val original = PixelBuffer.solid(900, 1200, PixelBuffer.rgba(12, 34, 56))
        val watermarked = Watermark.apply(original)
        val band = Watermark.bandFor(original.width, original.height)
        val changed = original.pixels.indices.any { index ->
            val x = index % original.width
            val y = index / original.width
            x in band.left until band.right && y in band.top until band.bottom &&
                original.pixels[index] != watermarked.pixels[index]
        }

        assertTrue("the existing watermark must alter the free document", changed)
    }

    @Test
    fun unlockedStudioDocumentSaveDoesNotRequireWatermark() {
        assertFalse(studioDocumentWatermarkRequired(serverConfirmedUnlock = true))
    }
}

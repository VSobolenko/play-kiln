package com.gexetr.plinthproductphotos.ui.components

import com.gexetr.plinthproductphotos.domain.wallet.WalletAccessPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSourceSelectionTest {
    @Test
    fun cameraPhotoIsNotLimitedByTheMultiSelectCap() {
        val cameraPhoto = listOf("camera-photo")

        assertEquals(
            cameraPhoto,
            importSourceSelection(PhotoSource.CAMERA, cameraPhoto, serverConfirmedUnlock = false),
        )
    }

    @Test
    fun filesUseTheSameClampAsGallery() {
        val selected = (1..10).toList()
        val expected = WalletAccessPolicy.clampSelection(selected, serverConfirmedUnlock = false)

        assertEquals(expected, importSourceSelection(PhotoSource.GALLERY, selected, false))
        assertEquals(expected, importSourceSelection(PhotoSource.FILES, selected, false))
    }
}

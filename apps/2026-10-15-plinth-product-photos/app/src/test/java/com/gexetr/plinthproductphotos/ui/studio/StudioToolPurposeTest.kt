package com.gexetr.plinthproductphotos.ui.studio

import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationId
import com.gexetr.plinthproductphotos.domain.studio.StudioOperationRequest
import com.gexetr.plinthproductphotos.domain.studio.UpscaleResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioToolPurposeTest {
    @Test
    fun everyListedToolHasItsOwnPurpose() {
        val purposes = listOf(
            StudioOperationId.RELIGHT,
            StudioOperationId.EXPAND_CANVAS,
            StudioOperationId.ERASE_REGION,
            StudioOperationId.UPSCALE,
            StudioOperationId.RECOLOUR,
            StudioOperationId.GHOST_MANNEQUIN,
            StudioOperationId.VIRTUAL_MODEL,
            StudioOperationId.SERIES,
        ).map { studioToolPurposeResource(StudioOperationRequest(it)) }
        assertEquals(8, purposes.toSet().size)
        assertTrue(purposes.all { it != 0 })
    }

    @Test
    fun upscalePurposesDistinguishTheTwoResolutions() {
        assertEquals(
            R.string.w6_studio_purpose_upscale_2k,
            studioToolPurposeResource(StudioOperationRequest(
                StudioOperationId.UPSCALE, upscaleResolution = UpscaleResolution.TWO_K,
            )),
        )
        assertEquals(
            R.string.w6_studio_purpose_upscale_4k,
            studioToolPurposeResource(StudioOperationRequest(
                StudioOperationId.UPSCALE, upscaleResolution = UpscaleResolution.FOUR_K,
            )),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedToolIsNotGivenAMisleadingPurpose() {
        studioToolPurposeResource(StudioOperationRequest(StudioOperationId.EXPORT_PACK))
    }
}

package com.gexetr.plinthproductphotos.domain

import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pipeline.EnhancedOperation
import com.gexetr.plinthproductphotos.domain.pipeline.FidelityDecision
import com.gexetr.plinthproductphotos.domain.pipeline.FidelityGuard
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Test

class FidelityGuardTest {
    @Test
    fun identicalPixelsAndSilhouetteAreDelivered() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140, 255))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })

        val result = FidelityGuard.evaluate(source, mask, source, EnhancedOperation.RELIGHT)

        assertEquals(FidelityDecision.DELIVERED, result.decision)
    }

    @Test
    fun materialItemPixelDriftIsRejected() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140, 255))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })
        val changed = PixelBuffer.solid(9, 9, PixelBuffer.rgba(230, 10, 10, 255))

        val result = FidelityGuard.evaluate(source, mask, changed, EnhancedOperation.RELIGHT)

        assertEquals(FidelityDecision.REJECTED, result.decision)
    }

    @Test
    fun upscaleWithMatchingAspectRatioIsComparedAtGuardResolution() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140, 255))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })

        val result = FidelityGuard.evaluate(
            source = source,
            sourceMask = mask,
            result = source.scale(18, 18),
            operation = EnhancedOperation.UPSCALE,
        )

        assertEquals(FidelityDecision.DELIVERED, result.decision)
    }

    @Test
    fun changedBrandTokenIsRejectedEvenWhenTextSimilarityIsHigh() {
        val source = PixelBuffer.solid(9, 9, PixelBuffer.rgba(30, 80, 140, 255))
        val mask = Mask(ByteArray(81) { 0xff.toByte() })

        val result = FidelityGuard.evaluate(
            source = source,
            sourceMask = mask,
            result = source,
            operation = EnhancedOperation.RELIGHT,
            sourceText = "Acme model 123",
            resultText = "Acne model 123",
            brandTokens = setOf("Acme"),
        )

        assertEquals(FidelityDecision.REJECTED, result.decision)
    }
}

package com.gexetr.plinthproductphotos.data.export

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidRasterEncoderTest {
    @Test
    fun opaqueDarkBackgroundStaysOpaqueAndKeepsChannels() {
        val rgba = PixelBuffer.rgba(0x11, 0x18, 0x16)
        val argb = rgbaToArgb(PixelBuffer.solid(1, 1, rgba).pixels[0])
        assertEquals("opaque pixel must stay opaque", 0xFF, argb ushr 24 and 0xff)
        assertEquals(0x11, argb ushr 16 and 0xff)
        assertEquals(0x18, argb ushr 8 and 0xff)
        assertEquals(0x16, argb and 0xff)
        assertEquals(0xFF111816.toInt(), argb)
    }

    @Test
    fun opaqueColourNeverGainsBlueCastOrTransparency() {
        val argb = rgbaToArgb(PixelBuffer.rgba(0x11, 0x18, 0x16))
        val alpha = argb ushr 24 and 0xff
        val red = argb ushr 16 and 0xff
        val blue = argb and 0xff
        assertTrue("alpha was $alpha", alpha == 255)
        assertTrue("blue-dominant cast: blue=$blue red=$red", blue <= red + 0x10)
    }

    @Test
    fun translucentAlphaIsPreserved() {
        val argb = rgbaToArgb(PixelBuffer.rgba(10, 20, 30, 128))
        assertEquals(0x800A141E.toInt(), argb)
    }
}

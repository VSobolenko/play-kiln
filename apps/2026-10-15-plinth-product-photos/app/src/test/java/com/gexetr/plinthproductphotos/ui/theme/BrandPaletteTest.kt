package com.gexetr.plinthproductphotos.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BrandPaletteTest {
    @Test
    fun composedSchemesCarryTheDeclaredBrandColours() {
        assertEquals(Color(0xFF63B7AA), PlinthPalette.Amber)
        assertEquals(Color(0xFF111816), PlinthPalette.Ink)
        assertEquals(Color(0xFF63B7AA), LightColorScheme.primary)
        assertEquals(Color(0xFF111816), DarkColorScheme.background)
    }

    @Test
    fun themeNeverDerivesColoursFromTheWallpaper() {
        val source = File("src/main/java/com/gexetr/plinthproductphotos/ui/theme/Theme.kt").readText()
        assertFalse(source.contains("dynamicLightColorScheme"))
        assertFalse(source.contains("dynamicDarkColorScheme"))
    }
}

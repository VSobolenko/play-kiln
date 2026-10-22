package com.gexetr.coverkit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationCatalogTest {

    @Test
    fun catalogContainsExactOwnerNetworkPixels() {
        assertEquals(1280 to 720, pixels("yt_thumb"))
        assertEquals(1080 to 1920, pixels("yt_shorts"))
        assertEquals(1080 to 1080, pixels("ig_post"))
        assertEquals(1080 to 1920, pixels("ig_story"))
        assertEquals(1080 to 1920, pixels("tiktok_cover"))
        assertEquals(1080 to 1920, pixels("wa_status"))
        assertEquals(820 to 312, pixels("fb_cover"))
        assertEquals(1500 to 500, pixels("x_header"))
        assertEquals(1584 to 396, pixels("linkedin_banner"))
        assertEquals(1000 to 1500, pixels("pinterest_pin"))
    }

    @Test
    fun catalogHasTenDestinationsWithStringLabels() {
        assertTrue(DestinationCatalog.all.size >= 10)
        DestinationCatalog.all.forEach { destination ->
            assertNotNull(destination.id)
            assertNotNull(destination.labelRes)
        }
    }

    @Test
    fun everyDestinationHasFiveFreeAndTwoPremiumTemplates() {
        DestinationCatalog.all.forEach { destination ->
            val templates = TemplateFactory.templatesFor(destination.id)
            assertTrue("${destination.id} needs five free templates", templates.count { it.isFree } >= 5)
            assertTrue("${destination.id} needs two premium templates", templates.count { !it.isFree } >= 2)
            assertEquals(
                "${destination.id} template names must be unique",
                templates.size,
                templates.map { it.name }.toSet().size,
            )
        }
    }

    private fun pixels(id: String): Pair<Int, Int> {
        val destination = DestinationCatalog.requireById(id)
        return destination.widthPx to destination.heightPx
    }
}

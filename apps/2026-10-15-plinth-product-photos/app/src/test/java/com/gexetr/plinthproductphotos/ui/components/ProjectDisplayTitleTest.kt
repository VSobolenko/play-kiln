package com.gexetr.plinthproductphotos.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDisplayTitleTest {
    @Test
    fun keepsHumanProjectNamesInsteadOfReplacingThemWithDates() {
        assertEquals("Blue ceramic mug", projectDisplayTitle(" Blue ceramic mug ", 0L))
        assertEquals("IMG_20261017.jpg", projectDisplayTitle("IMG_20261017.jpg", 0L))
    }

    @Test
    fun uuidFilenamesAndJobIdentifiersBecomeLocalizedDateTimeLabels() {
        val names = listOf(
            "a62f12d2-f9fc-43cd-bcd4-55de26fd5162.jpg",
            "A62F12D2-F9FC-43CD-BCD4-55DE26FD5162",
            "plinth-a62f12d2-f9fc-43cd-bcd4-55de26fd5162.png",
            "a62f12d2f9fc43cdbcd455de26fd5162.webp",
            "job_0123456789",
            "request-opaque-id",
            "",
        )

        for (locale in listOf(Locale.US, Locale.forLanguageTag("ru-RU"))) {
            for (name in names) {
                val label = projectDisplayTitle(name, 86_400_000L, locale)
                assertTrue(label.isNotBlank())
                assertTrue("A date label must retain its year: $label", label.contains("1970"))
                assertFalse(label.contains("a62f", ignoreCase = true))
                assertFalse(label.contains("job_", ignoreCase = true))
                assertFalse(label.contains("request-", ignoreCase = true))
            }
        }
    }
}

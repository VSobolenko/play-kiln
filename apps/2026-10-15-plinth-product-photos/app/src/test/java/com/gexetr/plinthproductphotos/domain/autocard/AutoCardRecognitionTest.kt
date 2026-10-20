package com.gexetr.plinthproductphotos.domain.autocard

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardRecognitionTest {
    @Test
    fun validRecognitionUsesOneTransportCall() = runBlocking {
        var calls = 0
        val client = AutoCardRecognitionClient {
            calls++
            """{"category":"bag","confidence":0.87,"attributes":["leather"],"visibleText":["SKU-4"],"scaleHint":{"kind":"hand"},"sku":"SKU-4"}"""
        }

        val result = client.recognize(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1))

        assertEquals(1, calls)
        assertEquals(AutoCategory.BAG, result.category)
        assertEquals(0.87f, result.confidence)
    }

    @Test
    fun schemaInvalidResponseFallsBackAfterExactlyOneTransportCall() = runBlocking {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2)
        var calls = 0
        val client = AutoCardRecognitionClient { request ->
            assertTrue(jpeg.contentEquals(request))
            calls++
            """{"category":"unknown","confidence":0.9,"attributes":[],"visibleText":[],"scaleHint":{"kind":"none"}}"""
        }

        val result = client.recognize(jpeg)

        assertEquals(1, calls)
        assertEquals(AutoCategory.OTHER, result.category)
        assertEquals(0f, result.confidence)
        assertTrue(result.attributes.isEmpty())
    }

    @Test
    fun recognitionFailureLeavesManualCategorySelectionUsable() = runBlocking {
        var calls = 0
        val client = AutoCardRecognitionClient {
            calls++
            throw IllegalStateException("recognition endpoint unavailable")
        }
        val selection = AutoCardSelection(
            category = AutoCategory.OTHER,
            categoryConfirmed = false,
            marketplaceId = "etsy",
        )
        var failed = false

        try {
            client.recognize(byteArrayOf(0xff.toByte(), 0xd8.toByte()))
        } catch (_: IllegalStateException) {
            failed = true
        }

        val manual = selection.chooseCategory(AutoCategory.BAG).confirmCategory()

        assertTrue(failed)
        assertEquals(1, calls)
        assertEquals(AutoCategory.BAG, manual.category)
        assertTrue(manual.categoryConfirmed)
    }

    @Test(expected = IllegalStateException::class)
    fun doesNotConvertTransportFailuresIntoARecognitionFallback() {
        runBlocking {
            var calls = 0
            val client = AutoCardRecognitionClient {
                calls++
                throw IllegalStateException("recognition endpoint unavailable")
            }

            client.recognize(byteArrayOf(0xff.toByte(), 0xd8.toByte()))
        }
    }
}

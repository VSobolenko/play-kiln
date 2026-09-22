package com.gexetr.capability.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingCatalogTest {
    @Test
    fun `valid product ids accept lowercase digits underscores and dots`() {
        assertNull(ProductId("premium_2.monthly").validate())
        assertNull(ProductId("2coins").validate())
    }

    @Test
    fun `invalid product ids are rejected`() {
        val invalid = listOf("", "_leading", ".leading", "Upper", "has-dash", "has space")

        invalid.forEach { value ->
            assertTrue("Expected '$value' to be invalid", ProductId(value).validate() != null)
        }
    }

    @Test
    fun `catalog reports invalid and duplicate products`() {
        val duplicate = ProductId("premium")
        val catalog = BillingCatalog(
            subscriptions = listOf(duplicate, ProductId("Bad")),
            inAppProducts = listOf(duplicate),
        )

        assertEquals(listOf(duplicate, ProductId("Bad"), duplicate), catalog.all())
        assertEquals(2, catalog.validate().size)
    }
}

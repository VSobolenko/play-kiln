package com.gexetr.capability.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `a consumable is identified and exposes its pack`() {
        val unlock = ProductId("premium")
        val pack = ConsumablePack(id = ProductId("tokens_100"), grants = 100, unit = "tokens")
        val catalog = BillingCatalog(inAppProducts = listOf(unlock), consumables = listOf(pack))

        assertTrue(catalog.isConsumable(pack.id))
        assertFalse(catalog.isConsumable(unlock))
        assertEquals(pack, catalog.packFor(pack.id))
        assertNull(catalog.packFor(unlock))
        assertEquals(setOf("tokens"), catalog.consumableUnits())
    }

    @Test
    fun `a pack with a non-positive grant or blank unit is rejected`() {
        val freebie = ConsumablePack(id = ProductId("tokens_free"), grants = 0, unit = "tokens")
        val unlabelled = ConsumablePack(id = ProductId("tokens_x"), grants = 10, unit = "  ")

        assertEquals(1, freebie.validate().size)
        assertEquals(1, unlabelled.validate().size)
    }

    @Test
    fun `an id reused across product kinds is a duplicate`() {
        // The same ID cannot be both an unlock and a pack: Play would reject it, and the local
        // grant rule would be ambiguous.
        val shared = ProductId("bonus")
        val catalog = BillingCatalog(
            inAppProducts = listOf(shared),
            consumables = listOf(ConsumablePack(id = shared, grants = 5, unit = "credits")),
        )

        assertTrue(catalog.validate().isNotEmpty())
    }

    @Test
    fun `only a server-cost pack demands a purchase authority`() {
        val local = BillingCatalog(
            consumables = listOf(ConsumablePack(ProductId("themes"), 1, "themes"))
        )
        val serverCost = BillingCatalog(
            consumables = listOf(
                ConsumablePack(ProductId("gen_10"), 10, "generations", Fulfillment.SERVER_COST)
            )
        )

        assertFalse(local.requiresPurchaseAuthority())
        assertTrue(serverCost.requiresPurchaseAuthority())
    }
}

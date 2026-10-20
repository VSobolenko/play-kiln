package com.gexetr.plinthproductphotos.data.look

import com.gexetr.plinthproductphotos.domain.model.BackgroundSpec
import com.gexetr.plinthproductphotos.domain.model.ShadowSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultLookTest {
    @Test
    fun freshLibraryHasASolidLookThatNeedsNoSceneAsset() {
        val looks = looksWithDefault(emptyList(), "Plinth")

        assertEquals(1, looks.size)
        val look = looks.single()
        assertEquals(BackgroundSpec.Kind.SOLID, look.background.kind)
        assertEquals(0xffffffff.toInt(), look.background.colorArgb)
        assertNull(look.background.sceneAssetPath)
        assertEquals(ShadowSpec.Kind.NONE, look.shadow.kind)
    }

    @Test
    fun existingLooksAreNotReplacedOrDuplicated() {
        val saved = looksWithDefault(emptyList(), "Plinth").single()
            .copy(id = "seller-look", name = "Seller's Look", createdAt = 123L)
        val existing = listOf(saved)

        assertSame(existing, looksWithDefault(existing, "Plinth"))
    }

    @Test
    fun repeatedSeedingKeepsTheSameLookAndLeavesUserLooksPreferred() {
        val initial = looksWithDefault(emptyList(), "Plinth")
        val again = looksWithDefault(initial, "Plinth")
        val saved = initial.single().copy(id = "seller-look", createdAt = 123L)

        assertEquals(initial, again)
        assertEquals(saved, looksWithDefault(initial + saved, "Plinth").maxBy { it.createdAt })
    }
}

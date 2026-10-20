package com.gexetr.plinthproductphotos.data.project

import com.gexetr.plinthproductphotos.ui.cutout.sessionPhotoToOpen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedOutputSelectionTest {
    @Test
    fun explicitOutputWinsOverTheLatestAutoCard() = runBlocking {
        val picked = selectSavedOutput<String>("out-b", { "by-id:$it" }, { "latest" })

        assertEquals("by-id:out-b", picked)
    }

    @Test
    fun missingExplicitOutputDoesNotFallBackToAnotherResult() = runBlocking {
        val picked = selectSavedOutput<String>("gone", { null }, { "latest" })

        assertNull(picked)
    }

    @Test
    fun withoutAnOutputIdTheLatestAutoCardIsUsed() = runBlocking {
        val picked = selectSavedOutput<String>(null, { error("must not look up by id") }, { "latest" })

        assertEquals("latest", picked)
    }

    @Test
    fun cutoutOpensTheRequestedPhotoElseTheNewest() {
        val newest = ProjectPhoto("p-new", "new", "sources/new", 2L)
        val older = ProjectPhoto("p-old", "old", "sources/old", 1L)

        assertEquals(older, sessionPhotoToOpen("p-old", listOf(newest, older)))
        assertEquals(newest, sessionPhotoToOpen(null, listOf(newest, older)))
        assertEquals(newest, sessionPhotoToOpen("unknown", listOf(newest, older)))
        assertNull(sessionPhotoToOpen("p-old", emptyList()))
    }
}

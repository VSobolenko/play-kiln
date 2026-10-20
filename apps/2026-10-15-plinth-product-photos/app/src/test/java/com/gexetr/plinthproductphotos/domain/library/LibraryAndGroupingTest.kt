package com.gexetr.plinthproductphotos.domain.library

import com.gexetr.plinthproductphotos.domain.group.GroupPhoto
import com.gexetr.plinthproductphotos.domain.group.Grouping
import com.gexetr.plinthproductphotos.domain.group.GroupOutputName
import com.gexetr.plinthproductphotos.domain.group.SkuPattern
import com.gexetr.plinthproductphotos.domain.group.SkuFileNamer
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAndGroupingTest {

    @Test
    fun regroupingGroupsPhotosByTheSellerSkuPattern() {
        val groups = Grouping.regroup(
            listOf(
                GroupPhoto("front", "shoe-2"),
                GroupPhoto("side", "shoe-1"),
                GroupPhoto("detail", "shoe-2"),
            ),
            SkuPattern("{base}-{n}"),
        )

        assertEquals(listOf("shoe-1", "shoe-2"), groups.map { it.sku })
        assertEquals(listOf("side"), groups[0].photos.map { it.id })
        assertEquals(listOf("front", "detail"), groups[1].photos.map { it.id })
    }

    @Test
    fun regroupingLeadsWithMainPhotosAndHonoursPhotoOrderWithinEachRole() {
        val groups = Grouping.regroup(
            listOf(
                GroupPhoto("extra-later", "shoe-1", photoOrder = 2, role = SlotRole.EXTRA),
                GroupPhoto("main-later", "shoe-1", photoOrder = 4),
                GroupPhoto("extra-first", "shoe-1", photoOrder = 1, role = SlotRole.EXTRA),
                GroupPhoto("main-first", "shoe-1", photoOrder = 3),
            ),
            SkuPattern("{base}-{n}"),
        )

        assertEquals(
            listOf("main-first", "main-later", "extra-first", "extra-later"),
            groups.single().photos.map { it.id },
        )
    }

    @Test
    fun movingPhotoUsesTheSameStableMainFirstOrderingAsRegrouping() {
        val pattern = SkuPattern("{base}-{n}")
        val groups = Grouping.regroup(
            listOf(
                GroupPhoto("z-front", "shoe-1"),
                GroupPhoto("a-detail", "shoe-1"),
                GroupPhoto("extra", "shoe-1", role = SlotRole.EXTRA),
                GroupPhoto("moved-main", "shoe-2"),
            ),
            pattern,
        )

        val moved = Grouping.movePhoto(groups, "moved-main", "shoe-1", pattern)

        assertEquals(
            listOf("z-front", "a-detail", "moved-main", "extra"),
            moved.single().photos.map { it.id },
        )
    }

    @Test
    fun skuNumberingIsUniqueAndStableAcrossARerun() {
        val pattern = SkuPattern("{base}-{n}")
        val first = pattern.assignUnique("cup", listOf("a", "b", "c"))
        val rerun = pattern.assignUnique("cup", listOf("a", "b", "c"))

        assertEquals(listOf("cup-1", "cup-2", "cup-3"), first)
        assertEquals(first, rerun)
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun referencePhotosAreNotInTheExportSet() {
        val groups = Grouping.regroup(
            listOf(
                GroupPhoto("main", "shoe-1"),
                GroupPhoto("ruler", "shoe-1", role = SlotRole.REFERENCE),
            ),
            SkuPattern("{base}-{n}"),
        )

        assertEquals(listOf("main"), groups.single().exportablePhotos().map { it.id })
    }

    @Test
    fun skuFilenamesAreUniqueAndStableWithinAGroup() {
        val outputs = listOf(
            GroupOutputName("main-1", SlotRole.MAIN, 1, "jpg"),
            GroupOutputName("extra-1", SlotRole.EXTRA, 1, ".png"),
            GroupOutputName("main-duplicate", SlotRole.MAIN, 1, "jpg"),
        )

        val first = SkuFileNamer.assign("shoe/2", outputs)
        val rerun = SkuFileNamer.assign("shoe/2", outputs)

        assertEquals(first, rerun)
        assertEquals(first.size, first.values.map(String::lowercase).toSet().size)
        assertTrue(first.values.all { it.startsWith("shoe-2_") })
    }

    @Test
    fun librarySearchMatchesSkuAndProjectText() {
        val projects = listOf(
            LibrarySearchEntry("1", "Studio lamps", listOf("lamp-4"), "Lighting"),
            LibrarySearchEntry("2", "Blue ceramics", listOf("vase-8"), "Home"),
        )

        assertEquals(listOf("1"), searchLibrary(projects, "LAMP-4").map { it.projectId })
        assertEquals(listOf("2"), searchLibrary(projects, "ceramics").map { it.projectId })
    }

    @Test
    fun clearingLibrarySearchQueryRestoresAllMatchingProjects() {
        val projects = listOf(
            LibrarySearchEntry("1", "Studio lamps", listOf("lamp-4"), "Lighting"),
            LibrarySearchEntry("2", "Blue ceramics", listOf("vase-8"), "Home"),
        )

        val searchQuery = librarySearchQuery(" lamp ")
        assertEquals(listOf("1"), searchLibrary(projects, searchQuery.orEmpty()).map { it.projectId })

        val clearedQuery = librarySearchQuery("")
        assertEquals(listOf("1", "2"), searchLibrary(projects, clearedQuery.orEmpty()).map { it.projectId })
    }

    @Test
    fun bulkMarketplaceChangeOnlyTouchesSelectionAndReportsEachFailure() {
        val selected = listOf("first", "bad", "third")
        val changed = mutableListOf<String>()

        val result = BulkOperationExecutor.changeMarketplace(selected, "amazon") { id, _ ->
            if (id == "bad") error("cannot update")
            changed += id
        }

        assertEquals(listOf("first", "third"), changed)
        assertEquals(listOf("first", "bad", "third"), result.map { it.itemId })
        assertTrue(result[0].succeeded)
        assertFalse(result[1].succeeded)
        assertTrue(result[2].succeeded)
    }
}

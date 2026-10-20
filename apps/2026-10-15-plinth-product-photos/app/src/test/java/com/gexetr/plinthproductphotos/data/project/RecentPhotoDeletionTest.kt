package com.gexetr.plinthproductphotos.data.project

import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentPhotoDeletionTest {
    @Test
    fun deleteProjectRemovesProjectFromRecentList() = runBlocking {
        val storage = MemoryRecentStorage()
        val recent = storage.open()
        recent.save(ProjectPhoto("old", "Old", "sources/old.jpg", 10L))
        recent.save(ProjectPhoto("deleted", "Deleted", "sources/deleted.jpg", 30L))
        recent.save(ProjectPhoto("new", "New", "sources/new.jpg", 20L))
        val databaseIds = mutableSetOf("old", "deleted", "new")

        deleteProjectFromIndexes("deleted", recent) {
            if (databaseIds.remove("deleted")) 1 else 0
        }

        assertEquals(setOf("old", "new"), databaseIds)
        assertEquals(listOf("new", "old"), storage.open().photos().map { it.id })
    }

    @Test
    fun failedDatabaseDeletionDoesNotRemoveRecentPhoto() = runBlocking {
        val recent = MemoryRecentStorage().open()
        recent.save(ProjectPhoto("kept", "Kept", "sources/kept.jpg", 10L))
        var rejected = false
        try {
            deleteProjectFromIndexes("kept", recent) { 0 }
        } catch (_: IllegalStateException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(listOf("kept"), recent.photos().map { it.id })
    }

    @Test
    fun recentIndexWriteFailureEscapesToTheRoomTransaction() = runBlocking {
        val storage = MemoryRecentStorage()
        val recent = storage.open()
        recent.save(ProjectPhoto("kept", "Kept", "sources/kept.jpg", 10L))
        storage.writable = false
        var rejected = false
        try {
            deleteProjectFromIndexes("kept", recent) { 1 }
        } catch (_: IOException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(listOf("kept"), recent.photos().map { it.id })
    }

    @Test
    fun libraryDeletionCallsSharedIndexRemovalInsideItsTransaction() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "registry/apps.json").isFile }
        val source = File(
            root,
            "apps/2026-10-15-plinth-product-photos/app/src/main/java/" +
                "com/gexetr/plinthproductphotos/data/repository/LibraryRepository.kt",
        ).readText().replace("\r\n", "\n")
        val deletion = source.substringAfter("suspend fun deleteProject(").substringBefore("suspend fun createGroup(")
        assertTrue(Regex("""database\.withTransaction\s*\{\s*deleteProjectFromIndexes\(""").containsMatchIn(deletion))
        assertTrue(deletion.contains("database.projectDao().deleteById(projectId)"))
    }

    private class MemoryRecentStorage {
        private var photos = emptyList<ProjectPhoto>()
        var writable = true
        fun open() = RecentPhotoStore(
            readPhotos = { photos.toList() },
            writePhotos = {
                if (!writable) throw IOException("Disk full")
                photos = it.toList()
            },
        )
    }
}

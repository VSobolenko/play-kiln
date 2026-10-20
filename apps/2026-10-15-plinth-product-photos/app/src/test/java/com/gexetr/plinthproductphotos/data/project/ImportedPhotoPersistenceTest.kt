package com.gexetr.plinthproductphotos.data.project

import androidx.paging.PagingSource
import com.gexetr.plinthproductphotos.data.db.ItemEntity
import com.gexetr.plinthproductphotos.data.db.ProjectDao
import com.gexetr.plinthproductphotos.data.db.ProjectEntity
import com.gexetr.plinthproductphotos.data.db.ProjectSummary
import com.gexetr.plinthproductphotos.data.look.looksWithDefault
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.ui.export.emptyReason
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedPhotoPersistenceTest {
    @Test
    fun importedPhotoCreatesAProjectAndExportableItemWithoutAnyOtherSave() = runBlocking {
        val dao = MemoryProjectDao()

        dao.importPhoto(photo)

        val project = requireNotNull(dao.byId("photo-1"))
        val item = requireNotNull(dao.importedItem("photo-1"))
        assertEquals("Product.jpg", project.name)
        assertEquals(123L, project.createdAt)
        assertEquals(123L, project.updatedAt)
        assertEquals("sources/photo-1.jpg", project.previewPath)
        assertEquals(project.id, item.projectId)
        assertEquals("sources/photo-1.jpg", item.sourcePath)
        assertEquals(SlotRole.MAIN, item.role)
        assertEquals(ItemState.IMPORTED, item.state)
        assertEquals(0, item.photoOrder)
        assertNull(item.maskPath)

        val looks = looksWithDefault(emptyList(), "Plinth")
        assertTrue(looks.isNotEmpty())
        assertNull(emptyReason(dao.projects.size, dao.items.size, looks.size, 1))
    }

    @Test
    fun registeringTheSameImportDoesNotReplaceProjectOrEraseItemEdits() = runBlocking {
        val dao = MemoryProjectDao()
        dao.importPhoto(photo)
        val editedProject = requireNotNull(dao.byId(photo.id)).copy(name = "Renamed")
        dao.update(editedProject)
        val editedItem = requireNotNull(dao.importedItem(photo.id)).copy(
            sku = "SKU-42",
            role = SlotRole.EXTRA,
            maskPath = "masks/photo-1.png",
            maskRevision = 2,
            state = ItemState.CUT,
        )
        dao.items[photo.id] = editedItem

        dao.importPhoto(photo)

        assertEquals(listOf(editedProject), dao.projects.values.toList())
        assertEquals(listOf(editedItem), dao.items.values.toList())
    }

    @Test
    fun independentImportsRemainSeparateProjects() = runBlocking {
        val dao = MemoryProjectDao()
        dao.importPhoto(photo)
        dao.importPhoto(photo.copy(id = "photo-2", sourcePath = "sources/photo-2.jpg"))

        assertEquals(setOf("photo-1", "photo-2"), dao.projects.keys)
        assertEquals(setOf("photo-1", "photo-2"), dao.items.values.map { it.projectId }.toSet())
    }

    private val photo = ProjectPhoto("photo-1", "Product.jpg", "sources/photo-1.jpg", 123L)

    private class MemoryProjectDao : ProjectDao {
        val projects = linkedMapOf<String, ProjectEntity>()
        val items = linkedMapOf<String, ItemEntity>()

        override suspend fun insertImportedProject(project: ProjectEntity) {
            projects.putIfAbsent(project.id, project)
        }

        override suspend fun insertImportedItem(item: ItemEntity) {
            check(projects.containsKey(item.projectId))
            items.putIfAbsent(item.id, item)
        }

        override suspend fun importedItem(itemId: String): ItemEntity? = items[itemId]

        override suspend fun byId(projectId: String): ProjectEntity? = projects[projectId]

        override suspend fun insert(project: ProjectEntity) {
            // REPLACE would cascade-delete the project's items in SQLite.
            items.entries.removeAll { it.value.projectId == project.id }
            projects[project.id] = project
        }

        override suspend fun update(project: ProjectEntity) {
            projects[project.id] = project
        }

        override fun pageLibrary(
            search: String?,
            category: String?,
            marketplaceId: String?,
            updatedAfter: Long?,
            updatedBefore: Long?,
        ): PagingSource<Int, ProjectSummary> = error("Not used by import persistence")

        override suspend fun categories(): List<String> = error("Not used by import persistence")
        override suspend fun marketplaces(): List<String> = error("Not used by import persistence")
        override suspend fun selectedProjects(projectIds: List<String>): List<ProjectEntity> =
            projects.values.filter { it.id in projectIds }

        override suspend fun deleteById(projectId: String): Int = error("Not used by import persistence")
    }
}

package com.gexetr.plinthproductphotos.data.project

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteException
import com.gexetr.plinthproductphotos.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class ProjectPhoto(
    val id: String,
    val name: String,
    val sourcePath: String,
    val importedAt: Long,
)

class ProjectRepository(context: Context) {
    private val appContext = context.applicationContext
    private val recent = RecentPhotoStore(appContext)
    private val sourceDirectory = File(appContext.filesDir, "sources")
    private val library = LibraryRepository(appContext)

    @Synchronized
    fun recentPhotos(): List<ProjectPhoto> = recent.photos()

    @Synchronized
    fun photo(id: String): ProjectPhoto? = recent.photos().firstOrNull { it.id == id }

    @Synchronized
    fun addPhoto(photo: ProjectPhoto) {
        sourceFile(photo.sourcePath)
        try {
            // Keep synchronous import success bound to the Room transaction's completion.
            runBlocking(Dispatchers.IO) {
                library.importPhoto(photo) { recent.save(photo) }
            }
        } catch (failure: SQLiteException) {
            throw IOException("Could not save imported photo to the library", failure)
        }
    }

    fun sourceFile(relativePath: String): File {
        val file = File(appContext.filesDir, relativePath).canonicalFile
        require(file.toPath().startsWith(sourceDirectory.canonicalFile.toPath())) {
            "Photo path is outside private source storage"
        }
        return file
    }
}

internal class RecentPhotoStore(
    private val readPhotos: () -> List<ProjectPhoto>,
    private val writePhotos: (List<ProjectPhoto>) -> Unit,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    private constructor(preferences: SharedPreferences) : this(
        readPhotos = { decodePhotos(preferences.getString(PHOTOS_KEY, null)) },
        writePhotos = { photos ->
            if (!preferences.edit().putString(PHOTOS_KEY, encodePhotos(photos)).commit()) {
                throw IOException("Could not save recent photo metadata")
            }
        },
    )

    fun photos(): List<ProjectPhoto> = synchronized(lock) {
        readPhotos().sortedByDescending(ProjectPhoto::importedAt)
    }

    fun save(photo: ProjectPhoto) = synchronized(lock) {
        writePhotos(readPhotos().filterNot { it.id == photo.id } + photo)
    }

    fun remove(projectId: String) = synchronized(lock) {
        writePhotos(readPhotos().filterNot { it.id == projectId })
    }

    companion object {
        const val PREFERENCES_NAME = "plinth_projects"
        private const val PHOTOS_KEY = "recent_photos"
        private val lock = Any()

        private fun encodePhotos(photos: List<ProjectPhoto>): String = JSONArray().apply {
            photos.forEach { photo ->
                put(JSONObject()
                    .put("id", photo.id)
                    .put("name", photo.name)
                    .put("sourcePath", photo.sourcePath)
                    .put("importedAt", photo.importedAt))
            }
        }.toString()

        private fun decodePhotos(stored: String?): List<ProjectPhoto> {
            if (stored == null) return emptyList()
            val json = JSONArray(stored)
            return List(json.length()) { index ->
                val entry = json.getJSONObject(index)
                ProjectPhoto(
                    id = entry.getString("id"),
                    name = entry.getString("name"),
                    sourcePath = entry.getString("sourcePath"),
                    importedAt = entry.getLong("importedAt"),
                )
            }
        }
    }
}

internal suspend fun deleteProjectFromIndexes(
    projectId: String,
    recent: RecentPhotoStore,
    deleteRow: suspend () -> Int,
) {
    check(deleteRow() == 1) { "Project '$projectId' changed while it was being deleted" }
    recent.remove(projectId)
}

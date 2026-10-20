package com.gexetr.plinthproductphotos.data.coach

import android.content.Context
import android.net.Uri
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.photoimport.PhotoImporter
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.model.Item
import com.gexetr.plinthproductphotos.domain.model.ItemState
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

data class CoachCaptureHandoff(
    val item: Item,
    val pixels: PixelBuffer,
)

class CoachCaptureRepository(context: Context) {
    private val appContext = context.applicationContext
    private val projects = ProjectRepository(appContext)
    private val importer = PhotoImporter(appContext, projects)

    fun importPickedPhoto(
        uri: Uri,
        projectId: String,
        category: String,
        role: SlotRole,
    ): CoachCaptureHandoff {
        require(projectId.isNotBlank()) { "Project id is required" }
        val photo = importer.importPhoto(uri)
        return handoff(photo, projectId, category, role)
    }

    fun importCameraFile(
        file: File,
        projectId: String,
        category: String,
        role: SlotRole,
    ): CoachCaptureHandoff {
        require(projectId.isNotBlank()) { "Project id is required" }
        require(file.isFile) { "Captured photo is missing" }
        val id = UUID.randomUUID().toString()
        val relativePath = "sources/$id.jpg"
        val destination = projects.sourceFile(relativePath)
        destination.parentFile?.let { directory ->
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create private photo storage")
            }
        }

        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            val photo = ProjectPhoto(
                id = id,
                name = appContext.getString(R.string.coach_camera_photo_name),
                sourcePath = relativePath,
                importedAt = System.currentTimeMillis(),
            )
            val handoff = handoff(photo, projectId, category, role)
            projects.addPhoto(photo)
            return handoff
        } catch (failure: Exception) {
            destination.delete()
            throw failure
        }
    }

    private fun handoff(
        photo: ProjectPhoto,
        projectId: String,
        category: String,
        role: SlotRole,
    ): CoachCaptureHandoff {
        val pixels = PhotoPixels.load(projects.sourceFile(photo.sourcePath))
        return CoachCaptureHandoff(
            item = Item(
                id = photo.id,
                projectId = projectId,
                sourcePath = photo.sourcePath,
                sku = null,
                groupId = null,
                role = role,
                category = category,
                categoryConfirmed = false,
                attributesJson = null,
                scaleHintJson = null,
                maskPath = null,
                maskRevision = 0,
                state = ItemState.IMPORTED,
                lastError = null,
            ),
            pixels = pixels,
        )
    }
}

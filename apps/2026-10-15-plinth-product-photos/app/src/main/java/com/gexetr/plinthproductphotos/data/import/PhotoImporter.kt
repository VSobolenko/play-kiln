package com.gexetr.plinthproductphotos.data.photoimport

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import org.json.JSONException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class PhotoImporter(context: Context, private val projects: ProjectRepository) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    fun importPhoto(uri: Uri): ProjectPhoto {
        val mimeType = resolver.getType(uri)
        require(mimeType?.startsWith("image/") == true) { "Selected file is not an image" }
        val displayName = displayName(uri) ?: "Product photo"
        val id = UUID.randomUUID().toString()
        val extension = extensionFor(mimeType)
        val relativePath = "sources/$id.$extension"
        val sourceFile = projects.sourceFile(relativePath)
        sourceFile.parentFile?.let { directory ->
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Could not create private photo storage")
            }
        }

        try {
            val input = resolver.openInputStream(uri)
                ?: throw IOException("The selected photo could not be opened")
            input.use { stream ->
                FileOutputStream(sourceFile).use { output -> stream.copyTo(output) }
            }
            val photo = ProjectPhoto(
                id = id,
                name = displayName,
                sourcePath = relativePath,
                importedAt = System.currentTimeMillis(),
            )
            projects.addPhoto(photo)
            return photo
        } catch (failure: IOException) {
            removeIncompleteCopy(sourceFile, failure)
            throw failure
        } catch (failure: SecurityException) {
            removeIncompleteCopy(sourceFile, failure)
            throw failure
        } catch (failure: IllegalArgumentException) {
            removeIncompleteCopy(sourceFile, failure)
            throw failure
        } catch (failure: JSONException) {
            removeIncompleteCopy(sourceFile, failure)
            throw failure
        }
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null
            else cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }

    private fun extensionFor(mimeType: String): String = when (mimeType.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        else -> "img"
    }

    private fun removeIncompleteCopy(file: java.io.File, cause: Exception) {
        if (file.exists() && !file.delete()) {
            throw IOException("Could not remove an incomplete private photo copy", cause)
        }
    }
}

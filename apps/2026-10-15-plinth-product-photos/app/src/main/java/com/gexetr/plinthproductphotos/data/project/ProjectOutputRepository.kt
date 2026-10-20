package com.gexetr.plinthproductphotos.data.project

import android.content.Context
import androidx.room.withTransaction
import com.gexetr.plinthproductphotos.data.db.OutputEntity
import com.gexetr.plinthproductphotos.data.db.PlinthDatabase
import com.gexetr.plinthproductphotos.data.export.AndroidRasterEncoder
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import com.gexetr.plinthproductphotos.ui.editor.CompositionSettings
import com.gexetr.plinthproductphotos.ui.editor.compositionSettingsFromJson
import com.gexetr.plinthproductphotos.ui.editor.toJsonObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

internal const val PROJECT_OUTPUT_DIRECTORY = "studio-results"

data class StoredProjectOutput(
    val record: OutputEntity,
    val pixels: PixelBuffer,
    val bytes: ByteArray,
)

class LegacyCardUnavailableException(message: String) : IOException(message)

class ProjectOutputRepository(
    context: Context,
    private val database: PlinthDatabase = PlinthDatabase.get(context),
) {
    private val appContext = context.applicationContext
    private val outputDirectory = File(appContext.filesDir, PROJECT_OUTPUT_DIRECTORY)

    suspend fun saveAutoCard(
        itemId: String,
        pixels: PixelBuffer,
        settings: CompositionSettings? = null,
    ): StoredProjectOutput {
        val item = requireNotNull(database.itemDao().byId(itemId)) {
            "Photo '$itemId' is no longer available"
        }
        val bytes = AndroidRasterEncoder().encode(pixels, format = "png", quality = 100)
        val writer = ProjectOutputWriter(outputDirectory) { output ->
            database.withTransaction {
                val currentItem = requireNotNull(database.itemDao().byId(item.id)) {
                    "Photo '${item.id}' was removed before its output could be saved"
                }
                val project = requireNotNull(database.projectDao().byId(currentItem.projectId)) {
                    "Project '${currentItem.projectId}' is no longer available"
                }
                database.outputDao().insert(output)
                database.projectDao().update(
                    project.copy(previewPath = output.path, updatedAt = output.createdAt),
                )
            }
        }
        val record = writer.saveAiCard(
            itemId = item.id,
            bytes = bytes,
            width = pixels.width,
            height = pixels.height,
            marketplaceId = item.marketplaceId,
            settings = settings,
        )
        return StoredProjectOutput(record, pixels, bytes)
    }

    suspend fun latestAutoCard(itemId: String): StoredProjectOutput? {
        val record = database.outputDao().byItem(itemId).firstOrNull {
            it.operationsJson.contains("\"operation\":\"auto-card\"")
        } ?: return null
        return readOwnedOutput(record)
    }

    suspend fun latestAutoCardSettings(itemId: String): CompositionSettings? {
        val record = database.outputDao().byItem(itemId).firstOrNull {
            it.operationsJson.contains("\"operation\":\"auto-card\"")
        } ?: return null
        val operations = runCatching {
            Json.parseToJsonElement(record.operationsJson) as? JsonObject
        }.getOrNull() ?: return null
        return compositionSettingsFromJson(operations["settings"] as? JsonObject)
    }

    suspend fun outputById(outputId: String): StoredProjectOutput? {
        val record = database.outputDao().byId(outputId) ?: return null
        return readOwnedOutput(record)
    }

    private fun readOwnedOutput(record: OutputEntity): StoredProjectOutput {
        val file = ownedOutputFile(record.path)
        if (!file.isFile) throw IOException("Saved AI card is missing: ${file.name}")
        val bytes = file.readBytes()
        val pixels = PhotoPixels.load(file)
        val sampleCount = minOf(pixels.pixels.size, 4096)
        var visibleSamples = 0
        repeat(sampleCount) { sample ->
            val pixelIndex = (sample.toLong() * pixels.pixels.size / sampleCount).toInt()
            if ((pixels.pixels[pixelIndex] and 0xff) > 32) visibleSamples++
        }
        if (visibleSamples * 100 < sampleCount) {
            throw LegacyCardUnavailableException("Saved AI card cannot be displayed: ${file.name}")
        }
        return StoredProjectOutput(record, pixels, bytes)
    }

    private fun ownedOutputFile(path: String): File {
        val root = outputDirectory.canonicalFile
        val file = File(path).canonicalFile
        require(file.parentFile == root) { "Saved AI card path is outside project output storage" }
        return file
    }
}

internal suspend fun <T> selectSavedOutput(
    outputId: String?,
    byId: suspend (String) -> T?,
    latest: suspend () -> T?,
): T? = if (outputId != null) byId(outputId) else latest()

internal class ProjectOutputWriter(
    private val outputDirectory: File,
    private val persist: suspend (OutputEntity) -> Unit,
) {
    suspend fun saveAiCard(
        itemId: String,
        bytes: ByteArray,
        width: Int,
        height: Int,
        marketplaceId: String?,
        settings: CompositionSettings? = null,
    ): OutputEntity {
        require(itemId.isNotBlank()) { "Project output item id must not be blank" }
        require(bytes.isNotEmpty()) { "Project output image must not be empty" }
        require(width > 0 && height > 0) { "Project output dimensions must be positive" }

        val directory = outputDirectory.canonicalFile
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Could not create project output storage")
        }
        val id = UUID.randomUUID().toString()
        val file = File(directory, "auto-card-$id.png").canonicalFile
        require(file.parentFile == directory) { "Project output path escaped its storage directory" }
        val temporary = File(directory, ".$id.tmp")
        try {
            FileOutputStream(temporary).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            if (!temporary.renameTo(file)) throw IOException("Could not finalize the project output image")
            val output = OutputEntity(
                id = id,
                itemId = itemId,
                path = file.path,
                width = width,
                height = height,
                bytes = file.length(),
                format = "png",
                provenance = Provenance.ENHANCED,
                operationsJson = buildJsonObject {
                    put("generated", true)
                    put("operation", "auto-card")
                    put("sourceItemId", itemId)
                    settings?.let { put("settings", it.toJsonObject()) }
                }.toString(),
                marketplaceId = marketplaceId,
                role = SlotRole.EXTRA,
                createdAt = System.currentTimeMillis(),
            )
            persist(output)
            return output
        } catch (failure: Exception) {
            temporary.delete()
            if (file.exists() && !file.delete()) {
                failure.addSuppressed(IOException("Could not remove an unregistered project output"))
            }
            throw failure
        }
    }
}

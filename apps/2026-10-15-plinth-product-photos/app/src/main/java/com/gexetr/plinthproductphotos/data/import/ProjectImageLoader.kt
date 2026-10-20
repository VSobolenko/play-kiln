package com.gexetr.plinthproductphotos.data.photoimport

import com.gexetr.plinthproductphotos.data.project.ProjectPhoto
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.domain.model.Mask
import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.io.FileNotFoundException

data class ProjectImage(
    val photo: ProjectPhoto,
    val source: PixelBuffer,
    val mask: Mask,
)

class ProjectImageLoader(private val projects: ProjectRepository) {
    fun load(id: String): ProjectImage {
        val photo = projects.photo(id) ?: throw FileNotFoundException("Imported photo is no longer available")
        val sourceFile = projects.sourceFile(photo.sourcePath)
        if (!sourceFile.isFile) throw FileNotFoundException("Imported photo file is missing")
        val source = PhotoPixels.load(sourceFile)
        return ProjectImage(
            photo = photo,
            source = source,
            mask = Mask(ByteArray(source.pixels.size) { 0xff.toByte() }),
        )
    }
}

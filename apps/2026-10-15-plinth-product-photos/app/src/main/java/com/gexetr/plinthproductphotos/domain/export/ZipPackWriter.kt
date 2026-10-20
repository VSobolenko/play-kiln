package com.gexetr.plinthproductphotos.domain.export

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipPackWriter {
    fun encode(pack: ExportPack): ByteArray =
        ByteArrayOutputStream().use { buffer ->
            write(pack, buffer)
            buffer.toByteArray()
        }

    fun write(pack: ExportPack, destination: OutputStream) {
        val names = (pack.entries.map { it.path } + "manifest.json")
        require(names.map { it.lowercase(Locale.ROOT) }.distinct().size == names.size) {
            "Pack contains duplicate ZIP paths"
        }
        val zip = ZipOutputStream(destination)
        pack.entries.forEach { item ->
            zip.putNextEntry(ZipEntry(item.path).apply { time = 0L })
            zip.write(item.artifact.bytes)
            zip.closeEntry()
        }
        zip.putNextEntry(ZipEntry("manifest.json").apply { time = 0L })
        zip.write(pack.manifestJson.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.finish()
        zip.flush()
    }
}

object MarketplaceBundleWriter {
    fun encode(packs: List<ExportPack>): ByteArray =
        ByteArrayOutputStream().use { output ->
            write(packs, output)
            output.toByteArray()
        }

    fun write(packs: List<ExportPack>, destination: OutputStream) {
        require(packs.isNotEmpty()) { "A marketplace bundle cannot be empty" }
        val names = packs.map { it.zipFileName.lowercase(Locale.ROOT) }
        require(names.distinct().size == names.size) { "Marketplace ZIP names collide in the bundle" }
        val zip = ZipOutputStream(destination)
        packs.forEach { pack ->
            zip.putNextEntry(ZipEntry(pack.zipFileName).apply { time = 0L })
            ZipPackWriter.write(pack, zip)
            zip.closeEntry()
        }
        zip.finish()
        zip.flush()
    }
}

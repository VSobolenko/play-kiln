package com.gexetr.plinthproductphotos.domain.studio

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)

private const val STUDIO_SOFTWARE = "com.gexetr.plinthproductphotos"
private const val AI_PROVENANCE = "AI-generated enhanced result"

internal fun addStudioPngProvenance(png: ByteArray): ByteArray {
    require(png.size >= PNG_SIGNATURE.size && png.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)) {
        "Invalid PNG signature"
    }

    var offset = PNG_SIGNATURE.size
    var hasHeader = false
    var insertAt = -1
    while (offset <= png.size - 12) {
        val length = readChunkLength(png, offset)
        require(length >= 0 && length <= png.size - offset - 12) { "Invalid PNG chunk length" }
        val type = png.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
        if (offset == PNG_SIGNATURE.size) {
            require(type == "IHDR" && length == 13) { "PNG must begin with IHDR" }
        }
        if (type == "IHDR") {
            require(!hasHeader) { "PNG contains multiple IHDR chunks" }
            hasHeader = true
        }
        if (type == "IDAT") {
            require(hasHeader) { "PNG is missing IHDR" }
            insertAt = offset
            break
        }
        if (type == "IEND") break
        offset += length + 12
    }
    require(insertAt >= 0) { "PNG is missing IDAT" }

    val metadataChunks = ByteArrayOutputStream().apply {
        write(textChunk("Software" to STUDIO_SOFTWARE))
        write(textChunk("AI-Provenance" to AI_PROVENANCE))
    }.toByteArray()
    return ByteArrayOutputStream(png.size + metadataChunks.size).use { output ->
        output.write(png, 0, insertAt)
        output.write(metadataChunks)
        output.write(png, insertAt, png.size - insertAt)
        output.toByteArray()
    }
}

private fun textChunk(entry: Pair<String, String>): ByteArray {
    val data = ByteArrayOutputStream()
    val (keyword, value) = entry
    data.write(keyword.toByteArray(Charsets.ISO_8859_1))
    data.write(0)
    data.write(value.toByteArray(Charsets.ISO_8859_1))
    val chunkData = data.toByteArray()
    val chunkType = "tEXt".toByteArray(Charsets.US_ASCII)
    val crc = CRC32().apply {
        update(chunkType)
        update(chunkData)
    }
    return ByteArrayOutputStream(chunkData.size + 12).use { output ->
        writeInt(output, chunkData.size)
        output.write(chunkType)
        output.write(chunkData)
        writeInt(output, crc.value.toInt())
        output.toByteArray()
    }
}

private fun readChunkLength(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)

private fun writeInt(output: ByteArrayOutputStream, value: Int) {
    output.write((value ushr 24) and 0xFF)
    output.write((value ushr 16) and 0xFF)
    output.write((value ushr 8) and 0xFF)
    output.write(value and 0xFF)
}

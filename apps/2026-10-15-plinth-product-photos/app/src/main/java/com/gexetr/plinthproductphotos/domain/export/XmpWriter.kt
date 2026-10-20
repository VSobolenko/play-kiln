package com.gexetr.plinthproductphotos.domain.export

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32

object XmpWriter {
    private const val JPEG_XMP_HEADER = "http://ns.adobe" + ".com/xap/1.0/\u0000"
    private const val PNG_XMP_KEYWORD = "XML:com" + ".adobe.xmp"
    private const val XMP_NAMESPACE = "http://ns.adobe" + ".com/xap/1.0/"
    private const val IPTC_NAMESPACE = "http://iptc.org/std/Iptc4xmpExt/2008-02-29/"

    fun packet(creatorTool: String, digitalSourceTypeUri: String?): String = buildString {
        append("""<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"><rdf:Description rdf:about=""")
        append("\"")
        append(""" xmlns:xmp="$XMP_NAMESPACE" xmlns:Iptc4xmpExt="$IPTC_NAMESPACE">""")
        append("<xmp:CreatorTool>")
        append(xmlEscape(creatorTool))
        append("</xmp:CreatorTool>")
        if (digitalSourceTypeUri != null) {
            append("<Iptc4xmpExt:DigitalSourceType>")
            append(xmlEscape(digitalSourceTypeUri))
            append("</Iptc4xmpExt:DigitalSourceType>")
        }
        append("</rdf:Description></rdf:RDF></x:xmpmeta>")
    }

    fun embed(encoded: ByteArray, format: String, packet: String): ByteArray =
        when (format.lowercase()) {
            "jpg", "jpeg" -> embedJpeg(encoded, packet)
            "png" -> embedPng(encoded, packet)
            "webp" -> embedWebp(encoded, packet)
            else -> throw IllegalArgumentException("XMP is not supported for $format")
        }

    private fun embedJpeg(encoded: ByteArray, packet: String): ByteArray {
        require(encoded.size >= 4 && encoded[0] == 0xff.toByte() && encoded[1] == 0xd8.toByte()) {
            "Encoder did not return a JPEG stream"
        }
        val header = JPEG_XMP_HEADER.toByteArray(StandardCharsets.UTF_8)
        val priorPacket = findJpegXmp(encoded, header)
        val mergedPacket = priorPacket?.let { mergePackets(it, packet) } ?: packet
        val xmp = mergedPacket.toByteArray(StandardCharsets.UTF_8)
        val payload = header + xmp
        require(payload.size + 2 <= 0xffff) { "XMP packet is too large for a JPEG APP1 segment" }
        val segment = ByteArrayOutputStream().apply {
            write(0xff)
            write(0xe1)
            write((payload.size + 2) ushr 8)
            write((payload.size + 2) and 0xff)
            write(payload)
        }.toByteArray()

        val output = ByteArrayOutputStream(encoded.size + segment.size)
        output.write(encoded, 0, 2)
        output.write(segment)
        var position = 2
        while (position < encoded.size) {
            val markerStart = position
            if (encoded[position] != 0xff.toByte()) {
                output.write(encoded, position, encoded.size - position)
                break
            }
            while (position < encoded.size && encoded[position] == 0xff.toByte()) position++
            if (position >= encoded.size) break
            val marker = encoded[position].toInt() and 0xff
            position++
            if (marker == 0xd9 || marker == 0xda) {
                output.write(encoded, markerStart, encoded.size - markerStart)
                break
            }
            if (marker == 0x01 || marker in 0xd0..0xd7) {
                output.write(encoded, markerStart, position - markerStart)
                continue
            }
            require(position + 2 <= encoded.size) { "Truncated JPEG marker" }
            val length = ((encoded[position].toInt() and 0xff) shl 8) or
                (encoded[position + 1].toInt() and 0xff)
            require(length >= 2 && position + length <= encoded.size) { "Invalid JPEG segment length" }
            val segmentEnd = position + length
            val isXmp = marker == 0xe1 && startsWith(encoded, position + 2, segmentEnd, header)
            if (!isXmp) output.write(encoded, markerStart, segmentEnd - markerStart)
            position = segmentEnd
        }
        return output.toByteArray()
    }

    private fun embedPng(encoded: ByteArray, packet: String): ByteArray {
        val signature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
        )
        require(startsWith(encoded, 0, encoded.size, signature)) { "Encoder did not return a PNG stream" }
        val output = ByteArrayOutputStream(encoded.size + packet.length)
        output.write(signature)
        val priorPacket = findPngXmp(encoded, signature.size)
        val mergedPacket = priorPacket?.let { mergePackets(it, packet) } ?: packet
        var position = signature.size
        var inserted = false
        while (position + 12 <= encoded.size) {
            val length = readBigEndianInt(encoded, position)
            require(length >= 0 && position.toLong() + length + 12 <= encoded.size) { "Invalid PNG chunk length" }
            val typeStart = position + 4
            val dataStart = position + 8
            val end = dataStart + length + 4
            val type = String(encoded, typeStart, 4, StandardCharsets.US_ASCII)
            val isXmp = type == "iTXt" && pngXmpKeyword(encoded, dataStart, dataStart + length)
            if (type == "IEND" && !inserted) {
                output.write(pngXmpChunk(mergedPacket))
                inserted = true
            }
            if (!isXmp) output.write(encoded, position, end - position)
            position = end
            if (type == "IEND") break
        }
        require(inserted) { "PNG has no IEND chunk" }
        require(position == encoded.size) { "Trailing bytes after PNG IEND" }
        return output.toByteArray()
    }

    private fun pngXmpChunk(packet: String): ByteArray {
        val packetBytes = packet.toByteArray(StandardCharsets.UTF_8)
        val data = PNG_XMP_KEYWORD.toByteArray(StandardCharsets.US_ASCII) +
            byteArrayOf(0, 0, 0, 0, 0) + packetBytes
        return pngChunk("iTXt", data)
    }

    private fun pngChunk(type: String, data: ByteArray): ByteArray {
        val typeBytes = type.toByteArray(StandardCharsets.US_ASCII)
        val checksum = CRC32().apply {
            update(typeBytes)
            update(data)
        }.value.toInt()
        return ByteArrayOutputStream(data.size + 12).apply {
            writeBigEndian(data.size)
            write(typeBytes)
            write(data)
            writeBigEndian(checksum)
        }.toByteArray()
    }

    private fun pngXmpKeyword(bytes: ByteArray, start: Int, end: Int): Boolean {
        val keyword = PNG_XMP_KEYWORD.toByteArray(StandardCharsets.US_ASCII)
        return startsWith(bytes, start, end, keyword) &&
            start + keyword.size < end &&
            bytes[start + keyword.size] == 0.toByte()
    }

    private fun embedWebp(encoded: ByteArray, packet: String): ByteArray {
        require(encoded.size >= 12 && String(encoded, 0, 4, StandardCharsets.US_ASCII) == "RIFF" &&
            String(encoded, 8, 4, StandardCharsets.US_ASCII) == "WEBP"
        ) { "Encoder did not return a WebP stream" }
        val priorPacket = findWebpXmp(encoded)
        val mergedPacket = priorPacket?.let { mergePackets(it, packet) } ?: packet
        val chunks = ByteArrayOutputStream()
        var position = 12
        while (position + 8 <= encoded.size) {
            val size = readLittleEndianInt(encoded, position + 4)
            require(size >= 0 && position.toLong() + 8 + size + (size and 1) <= encoded.size) {
                "Invalid WebP chunk size"
            }
            val end = position + 8 + size + (size and 1)
            val type = String(encoded, position, 4, StandardCharsets.US_ASCII)
            if (type != "XMP ") chunks.write(encoded, position, end - position)
            position = end
        }
        require(position == encoded.size) { "Trailing bytes after WebP chunks" }
        val xmp = mergedPacket.toByteArray(StandardCharsets.UTF_8)
        chunks.write("XMP ".toByteArray(StandardCharsets.US_ASCII))
        chunks.writeLittleEndian(xmp.size)
        chunks.write(xmp)
        if ((xmp.size and 1) != 0) chunks.write(0)
        val body = chunks.toByteArray()
        return ByteArrayOutputStream(body.size + 12).apply {
            write("RIFF".toByteArray(StandardCharsets.US_ASCII))
            writeLittleEndian(4 + body.size)
            write("WEBP".toByteArray(StandardCharsets.US_ASCII))
            write(body)
        }.toByteArray()
    }

    private fun findJpegXmp(encoded: ByteArray, header: ByteArray): String? {
        var position = 2
        while (position < encoded.size && encoded[position] == 0xff.toByte()) {
            while (position < encoded.size && encoded[position] == 0xff.toByte()) position++
            if (position >= encoded.size) return null
            val marker = encoded[position].toInt() and 0xff
            position++
            if (marker == 0xda || marker == 0xd9) return null
            if (marker == 0x01 || marker in 0xd0..0xd7) continue
            require(position + 2 <= encoded.size) { "Truncated JPEG marker" }
            val length = ((encoded[position].toInt() and 0xff) shl 8) or
                (encoded[position + 1].toInt() and 0xff)
            require(length >= 2 && position + length <= encoded.size) { "Invalid JPEG segment length" }
            val end = position + length
            if (marker == 0xe1 && startsWith(encoded, position + 2, end, header)) {
                val packetStart = position + 2 + header.size
                return String(encoded, packetStart, end - packetStart, StandardCharsets.UTF_8)
            }
            position = end
        }
        return null
    }

    private fun findPngXmp(encoded: ByteArray, start: Int): String? {
        var position = start
        while (position + 12 <= encoded.size) {
            val length = readBigEndianInt(encoded, position)
            require(length >= 0 && position.toLong() + length + 12 <= encoded.size) { "Invalid PNG chunk length" }
            val type = String(encoded, position + 4, 4, StandardCharsets.US_ASCII)
            val dataStart = position + 8
            if (type == "iTXt" && pngXmpKeyword(encoded, dataStart, dataStart + length)) {
                val textStart = dataStart + PNG_XMP_KEYWORD.length + 5
                return String(encoded, textStart, dataStart + length - textStart, StandardCharsets.UTF_8)
            }
            position += length + 12
            if (type == "IEND") break
        }
        return null
    }

    private fun findWebpXmp(encoded: ByteArray): String? {
        var position = 12
        while (position + 8 <= encoded.size) {
            val length = readLittleEndianInt(encoded, position + 4)
            require(length >= 0 && position.toLong() + 8 + length + (length and 1) <= encoded.size) {
                "Invalid WebP chunk size"
            }
            if (String(encoded, position, 4, StandardCharsets.US_ASCII) == "XMP ") {
                return String(encoded, position + 8, length, StandardCharsets.UTF_8)
            }
            position += 8 + length + (length and 1)
        }
        return null
    }

    private fun mergePackets(existing: String, generated: String): String {
        val description = Regex(
            "<rdf:Description\\b([^>]*)>(.*?)</rdf:Description>",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val oldMatch = description.find(existing)
            ?: throw IllegalArgumentException("Existing XMP has no mergeable RDF description")
        val newMatch = description.find(generated)
            ?: throw IllegalArgumentException("Generated XMP has no RDF description")
        var attributes = oldMatch.groupValues[1]
            .replace(Regex("\\s+xmp:CreatorTool=\"[^\"]*\""), "")
            .replace(Regex("\\s+Iptc4xmpExt:DigitalSourceType=\"[^\"]*\""), "")
        if (!attributes.contains("xmlns:xmp=")) attributes += " xmlns:xmp=\"$XMP_NAMESPACE\""
        if (!attributes.contains("xmlns:Iptc4xmpExt=")) {
            attributes += " xmlns:Iptc4xmpExt=\"$IPTC_NAMESPACE\""
        }
        val preserved = oldMatch.groupValues[2]
            .replace(Regex("<xmp:CreatorTool>.*?</xmp:CreatorTool>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(
                Regex("<Iptc4xmpExt:DigitalSourceType>.*?</Iptc4xmpExt:DigitalSourceType>", RegexOption.DOT_MATCHES_ALL),
                "",
            )
        val generatedProperties = newMatch.groupValues[2]
        val merged = "<rdf:Description$attributes>$preserved$generatedProperties</rdf:Description>"
        return existing.replaceRange(oldMatch.range, merged)
    }

    private fun startsWith(bytes: ByteArray, start: Int, end: Int, prefix: ByteArray): Boolean =
        end - start >= prefix.size && prefix.indices.all { bytes[start + it] == prefix[it] }

    private fun readBigEndianInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 24) or
            ((bytes[offset + 1].toInt() and 0xff) shl 16) or
            ((bytes[offset + 2].toInt() and 0xff) shl 8) or
            (bytes[offset + 3].toInt() and 0xff)

    private fun readLittleEndianInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArrayOutputStream.writeBigEndian(value: Int) {
        write(value ushr 24)
        write(value ushr 16)
        write(value ushr 8)
        write(value)
    }

    private fun ByteArrayOutputStream.writeLittleEndian(value: Int) {
        write(value)
        write(value ushr 8)
        write(value ushr 16)
        write(value ushr 24)
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

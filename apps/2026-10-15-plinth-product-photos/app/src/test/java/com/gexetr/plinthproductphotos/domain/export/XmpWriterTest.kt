package com.gexetr.plinthproductphotos.domain.export

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmpWriterTest {
    @Test
    fun jpegXmpIsMergedWithoutDroppingExistingFieldsOrDuplicatingThePacket() {
        val oldPacket = """<x:xmpmeta><rdf:RDF><rdf:Description rdf:about="" xmlns:custom="urn:test"><custom:Keep>kept</custom:Keep></rdf:Description></rdf:RDF></x:xmpmeta>"""
        val oldXmp = oldPacket.toByteArray(StandardCharsets.UTF_8)
        val header = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(StandardCharsets.UTF_8)
        val payload = header + oldXmp
        val length = payload.size + 2
        val jpeg = byteArrayOf(
            0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xe1.toByte(),
            (length ushr 8).toByte(), length.toByte(),
        ) + payload + byteArrayOf(0xff.toByte(), 0xd9.toByte())

        val result = XmpWriter.embed(
            jpeg,
            "jpg",
            XmpWriter.packet("Plinth 0.1.0", "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
        )
        val encoded = String(result, StandardCharsets.ISO_8859_1)

        assertTrue(encoded.contains("<custom:Keep>kept</custom:Keep>"))
        assertEquals(1, encoded.split("http://ns.adobe.com/xap/1.0/\u0000").size - 1)
        assertEquals(1, encoded.split("<xmp:CreatorTool>").size - 1)
        assertTrue(encoded.contains("trainedAlgorithmicMedia"))
    }
}

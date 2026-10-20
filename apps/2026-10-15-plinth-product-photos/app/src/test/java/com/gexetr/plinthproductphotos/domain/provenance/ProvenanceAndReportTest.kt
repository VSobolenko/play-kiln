package com.gexetr.plinthproductphotos.domain.provenance

import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.ApiResult
import com.gexetr.plinthproductphotos.data.api.ApiTransport
import com.gexetr.plinthproductphotos.data.api.TransportRequest
import com.gexetr.plinthproductphotos.data.api.TransportResponse
import com.gexetr.plinthproductphotos.domain.model.Provenance
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvenanceAndReportTest {
    @Test
    fun declarationsFollowTheTransformationThatProducedTheOutput() {
        val expected = mapOf(
            ProvenanceOperation.LOCAL_ORIGINAL to
                (Provenance.ORIGINAL_PIXELS to null),
            ProvenanceOperation.MANUAL_ADJUSTMENT to
                (Provenance.MANUALLY_ADJUSTED to "http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced"),
            ProvenanceOperation.GENERATED_SCENE to
                (Provenance.COMPOSITE_GENERATED_BACKGROUND to "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia"),
            ProvenanceOperation.EXPAND_CANVAS to
                (Provenance.COMPOSITE_GENERATED_BACKGROUND to "http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia"),
            ProvenanceOperation.RELIGHT to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
            ProvenanceOperation.RECOLOUR to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
            ProvenanceOperation.UPSCALE to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
            ProvenanceOperation.ERASE_REGION to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
            ProvenanceOperation.GHOST_MANNEQUIN to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
            ProvenanceOperation.VIRTUAL_MODEL to
                (Provenance.ENHANCED to "http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"),
        )

        expected.forEach { (operation, declaration) ->
            val actual = declarationFor(operation)
            assertEquals(operation.name, declaration.first, actual.provenance)
            assertEquals(operation.name, declaration.second, actual.digitalSourceType)
            if (actual.provenance == Provenance.ENHANCED) {
                assertNotNull("Enhanced output must retain a file declaration", actual.digitalSourceType)
                assertTrue(
                    operation.name,
                    actual.xmpPacket("Plinth AI: Product Photos", "1.2")
                        .contains(actual.digitalSourceType.orEmpty()),
                )
                assertTrue(
                    actual.xmpPacket("Plinth AI: Product Photos", "1.2")
                        .contains("<xmp:CreatorTool>Plinth AI: Product Photos 1.2</xmp:CreatorTool>"),
                )
            }
        }
    }

    @Test
    fun reportUsesApi11FieldsAndSuccessfulDuplicatesReturnTheRecordedState() {
        val transport = ReportTransport()
        val client = ReportApiClient("https://unit.test/v1", transport)

        val first = client.submit(
            jobId = "job-42",
            category = ReportCategory.OFFENSIVE,
            note = "Contains offensive imagery",
            deviceToken = "device-token",
        )
        val duplicate = client.submit(
            jobId = "job-42",
            category = ReportCategory.OFFENSIVE,
            note = "Contains offensive imagery",
            deviceToken = "device-token",
        )

        assertEquals(first, duplicate)
        assertEquals(1, transport.requests.size)
        val request = transport.requests.single()
        assertEquals("POST", request.method)
        assertEquals("https://unit.test/v1/reports", request.url)
        assertEquals("Bearer device-token", request.headers["Authorization"])
        val body = ApiJson.parseToJsonElement(
            String(requireNotNull(request.body), StandardCharsets.UTF_8),
        ).jsonObject
        assertEquals("job-42", body.getValue("jobId").jsonPrimitive.content)
        assertEquals("OFFENSIVE", body.getValue("category").jsonPrimitive.content)
        assertEquals("Contains offensive imagery", body.getValue("note").jsonPrimitive.content)
        assertEquals(setOf("jobId", "category", "note"), body.keys)
        assertEquals(
            ReportResponse(id = 9, status = ReportStatus.OPEN),
            (first as ApiResult.Success).value,
        )
    }

    @Test
    fun reportReadbackUsesTheServerStatusRoute() {
        val transport = ReportTransport()
        val client = ReportApiClient("https://unit.test/v1", transport)

        val result = client.getStatus(reportId = 9, deviceToken = "device-token")

        assertEquals(
            ReportStatusResponse(
                id = 9,
                status = ReportStatus.OPEN,
                category = ReportCategory.RIGHTS,
                createdAt = 1_797_000_000_000,
                caseStatus = "ESCALATED",
            ),
            (result as ApiResult.Success).value,
        )
        assertEquals("GET", transport.requests.single().method)
        assertEquals("https://unit.test/v1/reports/9", transport.requests.single().url)
        assertEquals("Bearer device-token", transport.requests.single().headers["Authorization"])
    }

    @Test
    fun localizedProvenanceCopyHasMatchingKeysAndNoCredentialsClaim() {
        val english = readStrings(Path.of("src/main/res/values/strings_provenance.xml"))
        val russian = readStrings(Path.of("src/main/res/values-ru/strings_provenance.xml"))

        assertEquals(english.keys, russian.keys)
        assertEquals(english.mapValues { formatArguments(it.value) }, russian.mapValues { formatArguments(it.value) })
        assertTrue(english.values.none { it.contains("c2pa", ignoreCase = true) })
        assertTrue(russian.values.none { it.contains("c2pa", ignoreCase = true) })
        assertEquals("AI-enhanced", english.getValue("provenance_enhanced_label"))
        assertFalse(english.getValue("provenance_enhanced_label").contains("credentials", ignoreCase = true))
    }

    private fun readStrings(path: Path): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = Files.newInputStream(path).use { input ->
            factory.newDocumentBuilder().parse(input)
        }
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    private fun formatArguments(value: String): List<String> =
        Regex("%(?:\\d+\\$)?[a-zA-Z%]").findAll(value).map { it.value }.toList()

    private class ReportTransport : ApiTransport {
        val requests = mutableListOf<TransportRequest>()

        override fun execute(request: TransportRequest): TransportResponse {
            requests += request
            if (request.method == "GET") {
                return TransportResponse(
                    statusCode = 200,
                    headers = emptyMap(),
                    body = """{"id":9,"status":"OPEN","category":"RIGHTS","createdAt":1797000000000,"caseStatus":"ESCALATED"}"""
                        .toByteArray(StandardCharsets.UTF_8),
                )
            }
            return TransportResponse(
                statusCode = 201,
                headers = emptyMap(),
                body = """{"id":9,"status":"OPEN"}""".toByteArray(StandardCharsets.UTF_8),
            )
        }
    }
}

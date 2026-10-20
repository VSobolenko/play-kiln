package com.gexetr.plinthproductphotos.ui.autocard

import com.gexetr.plinthproductphotos.data.api.ApiClient
import com.gexetr.plinthproductphotos.data.api.ApiJson
import com.gexetr.plinthproductphotos.data.api.ApiTransport
import com.gexetr.plinthproductphotos.data.api.JobRequest
import com.gexetr.plinthproductphotos.data.api.OperationId
import com.gexetr.plinthproductphotos.data.api.TransportRequest
import com.gexetr.plinthproductphotos.data.api.TransportResponse
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardEngine
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardFlowState
import com.gexetr.plinthproductphotos.domain.autocard.AutoCardSceneCatalog
import com.gexetr.plinthproductphotos.domain.autocard.AutoCategory
import com.gexetr.plinthproductphotos.domain.presets.EditPolicy
import com.gexetr.plinthproductphotos.domain.presets.MarketplacePresetParser
import com.gexetr.plinthproductphotos.domain.presets.PresetParseResult
import java.io.File
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCardGeneratedRecognitionTest {
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2)
    private val catalog = AutoCardSceneCatalog.fromJson(
        File("src/main/assets/autocard/scene-recipes.json").readText(),
    )
    private val preset = (MarketplacePresetParser.parseDocument(
        File("src/main/assets/marketplaces.json").readText(),
    ) as PresetParseResult.Success).document.marketplaces.first { it.editPolicy == EditPolicy.FREE }
    private val answer = """{"category":"bag","confidence":0.91,"attributes":["leather"],"visibleText":[],"scaleHint":{"kind":"ruler","valueMm":120},"sku":null}"""

    @Test
    fun recognizedItemControlsTheRealGeneratedJobPrompt() {
        val sent = mutableListOf<TransportRequest>()
        val api = client {
            sent += it
            if (it.url.endsWith("/recognize")) response(answer)
            else TransportResponse(202, emptyMap(), """{"jobId":"scene-1","reserved":2,"state":"QUEUED"}""".toByteArray())
        }
        val flow = recognizePhotoForAutoCard(api, jpeg, "session-token")
        val prompt = requireNotNull(AutoCardEngine.generationPrompt(flow, catalog, preset, null, "sunlit table"))
        api.submitJob(
            JobRequest("scene-request", OperationId.GENERATED_SCENE, listOf("source-upload"),
                buildJsonObject { put("prompt", prompt) }, 2),
            "session-token",
        )

        assertEquals(AutoCategory.BAG, flow.category)
        assertEquals("https://unit.test/v1/recognize", sent[0].url)
        assertEquals("POST", sent[0].method)
        assertTrue(jpeg.contentEquals(sent[0].body))
        assertEquals("Bearer session-token", sent[0].headers["Authorization"])
        assertEquals("image/jpeg", sent[0].headers["Content-Type"])
        val job = ApiJson.decodeFromString<JobRequest>(requireNotNull(sent[1].body).toString(Charsets.UTF_8))
        assertEquals(OperationId.GENERATED_SCENE, job.op)
        val submitted = job.params.getValue("prompt").jsonPrimitive.content
        assertTrue(submitted.contains("Product category: bag"))
        assertTrue(submitted.contains("leather"))
        assertTrue(submitted.contains("ruler; valueMm=120.0"))
        assertTrue(submitted.contains("Shadow: DROP"))
        assertTrue(submitted.contains("sunlit table"))
        assertTrue(submitted.length <= 2000)
    }

    @Test
    fun failedMalformedAndZeroConfidenceResponsesRequireSellerCategory() {
        val apis = listOf(
            client { throw IOException("offline") },
            client { TransportResponse(503, emptyMap(), """{"error":{"code":"PROVIDER_FAILED","message":"unavailable"}}""".toByteArray()) },
            client { response("""{"category":"not-a-category"}""") },
            client { response(answer.replace("0.91", "0")) },
        )
        apis.forEach { api ->
            val flow = recognizePhotoForAutoCard(api, jpeg, "session-token")
            assertNull(flow.category)
            assertNull(flow.recognition)
            assertNull(AutoCardEngine.generationPrompt(flow, catalog, preset, null, ""))
            val manual = flow.chooseCategory(AutoCategory.SHOES)
            assertTrue(requireNotNull(AutoCardEngine.generationPrompt(manual, catalog, preset, null, ""))
                .contains("Product category: shoes"))
        }
    }

    @Test
    fun sellerSceneAndCategoryOverrideRecognitionWithoutInventedScale() {
        val flow = recognizePhotoForAutoCard(client { response(answer) }, jpeg, "session-token")
        val first = requireNotNull(AutoCardEngine.generationPrompt(flow, catalog, preset, null, ""))
        val recipes = AutoCardEngine.generatedRecipes(AutoCategory.BAG, catalog)
        val second = requireNotNull(AutoCardEngine.generationPrompt(flow, catalog, preset, recipes[1].sceneId, ""))
        assertNotEquals(first, second)
        val edited = flow.chooseCategory(AutoCategory.SHOES).editAttributes(listOf("canvas"))
        val manual = requireNotNull(AutoCardEngine.generationPrompt(edited, catalog, preset, null, ""))
        assertTrue(manual.contains("Product category: shoes"))
        assertTrue(manual.contains("canvas"))
        assertFalse(manual.contains("leather"))
        assertFalse(manual.contains("valueMm"))
        assertNull(AutoCardEngine.generationPrompt(edited, catalog, preset, recipes[1].sceneId, ""))
    }

    @Test
    fun marketplaceWhiteAndOriginalOnlyPoliciesRemainExplicit() {
        val flow = AutoCardFlowState.offline().chooseCategory(AutoCategory.JEWELRY)
        val white = preset.copy(main = preset.main.copy(backgroundPolicy = "white-255"))
        val prompt = requireNotNull(AutoCardEngine.generationPrompt(flow, catalog, white, null, ""))
        assertTrue(prompt.contains("pure white (#FFFFFF)"))
        assertNull(AutoCardEngine.generationPrompt(flow, catalog, preset.copy(editPolicy = EditPolicy.ORIGINAL_ONLY), null, ""))
        assertNull(AutoCardEngine.generationPrompt(AutoCardFlowState.offline(), catalog, preset, null, ""))
    }

    @Test
    fun freeformPromptAloneBuildsAPromptWithoutCategoryOrRecipe() {
        val none = AutoCardFlowState.offline()
        assertNull(AutoCardEngine.generationPrompt(none, catalog, preset, null, "  "))
        val sea = requireNotNull(AutoCardEngine.generationPrompt(none, catalog, preset, null, "sea"))
        assertTrue(sea.contains("Seller background idea: sea"))
        assertFalse(sea.contains("Product category"))
        val forest = requireNotNull(AutoCardEngine.generationPrompt(none, catalog, preset, null, "forest for a tent"))
        assertTrue(forest.contains("forest for a tent"))
        assertTrue(sea.length <= 2000)
        assertNull(AutoCardEngine.generationPrompt(none, catalog, preset.copy(editPolicy = EditPolicy.ORIGINAL_ONLY), null, "sea"))
    }

    private fun response(body: String) = TransportResponse(200, emptyMap(), body.toByteArray())

    private fun client(execute: (TransportRequest) -> TransportResponse) =
        ApiClient("https://unit.test/v1", object : ApiTransport {
            override fun execute(request: TransportRequest): TransportResponse = execute.invoke(request)
        })
}

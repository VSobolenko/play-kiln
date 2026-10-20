package com.gexetr.plinthproductphotos.domain.export

import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.lint.ImageFacts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ZipPackWriterTest {
    @Test
    fun marketplaceZipContainsImagesInItemFoldersAndOverrideManifest() {
        val artifact = ExportArtifact(
            fileName = "A-014_main_1.jpg",
            width = 900,
            height = 1200,
            format = "jpg",
            quality = 90,
            bytes = byteArrayOf(1, 2, 3),
            provenance = Provenance.ORIGINAL_PIXELS,
            processing = ImageProcessing.ORIGINAL,
            digitalSourceTypeUri = null,
            xmpMetadata = "<xmp/>",
            hasAlpha = false,
            measuredFacts = ImageFacts(900, 1200, 0.75, "jpg", 3),
        )
        val pack = ExportPack(
            marketplaceId = "ozon",
            marketplaceName = "Ozon",
            rulesVerifiedAt = "2026-10-14",
            rulesSource = "https://example.org/rules",
            zipFileName = "ozon.zip",
            entries = listOf(ExportPackEntry("A-014/A-014_main_1.jpg", artifact)),
            manifestJson = """{"files":[{"overrides":[{"ruleId":"L05","measuredValue":"ratio=0.75"}]}]}""",
        )

        val entries = ZipInputStream(ByteArrayInputStream(ZipPackWriter.encode(pack))).use { zip ->
            buildMap<String, String> {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    put(entry.name, zip.readBytes().toString(Charsets.UTF_8))
                }
            }
        }

        assertEquals(setOf("A-014/A-014_main_1.jpg", "manifest.json"), entries.keys)
        assertEquals("\u0001\u0002\u0003", entries.getValue("A-014/A-014_main_1.jpg"))
        assertEquals(pack.manifestJson, entries.getValue("manifest.json"))
    }

    @Test
    fun oneBundleContainsOneIndependentPackForEachMarketplace() {
        val artifact = ExportArtifact(
            fileName = "A-014_main_1.jpg",
            width = 900,
            height = 1200,
            format = "jpg",
            quality = 90,
            bytes = byteArrayOf(1, 2, 3),
            provenance = Provenance.ORIGINAL_PIXELS,
            processing = ImageProcessing.ORIGINAL,
            digitalSourceTypeUri = null,
            xmpMetadata = "<xmp/>",
            hasAlpha = false,
            measuredFacts = ImageFacts(900, 1200, 0.75, "jpg", 3),
        )
        val packs = listOf("ozon", "etsy").map { marketplace ->
            ExportPack(
                marketplaceId = marketplace,
                marketplaceName = marketplace,
                rulesVerifiedAt = "2026-10-14",
                rulesSource = "https://example.org/rules",
                zipFileName = "$marketplace.zip",
                entries = listOf(ExportPackEntry("A-014/A-014_main_1.jpg", artifact)),
                manifestJson = """{"marketplace":"$marketplace"}""",
            )
        }

        val bundleEntries = ZipInputStream(ByteArrayInputStream(MarketplaceBundleWriter.encode(packs))).use { zip ->
            buildMap<String, ByteArray> {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    put(entry.name, zip.readBytes())
                }
            }
        }
        assertEquals(setOf("ozon.zip", "etsy.zip"), bundleEntries.keys)
        packs.forEach { pack ->
            val packEntries = ZipInputStream(ByteArrayInputStream(bundleEntries.getValue(pack.zipFileName))).use { zip ->
                buildSet {
                    while (true) add(zip.nextEntry?.name ?: break)
                }
            }
            assertEquals(setOf("A-014/A-014_main_1.jpg", "manifest.json"), packEntries)
        }
    }
}

package com.gexetr.plinthproductphotos.data.project

import com.gexetr.plinthproductphotos.domain.model.Provenance
import com.gexetr.plinthproductphotos.domain.model.SlotRole
import com.gexetr.plinthproductphotos.data.db.OutputEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectOutputWriterTest {
    @Test
    fun savesAiCardAsDurableProjectOutput() = runBlocking {
        val directory = File("build/tmp/project-output-${UUID.randomUUID()}")
        var persisted = false
        try {
            var stored: OutputEntity? = null
            val writer = ProjectOutputWriter(directory) { output ->
                stored = output
                persisted = true
            }
            val bytes = byteArrayOf(1, 2, 3, 4)

            val output = writer.saveAiCard(
                itemId = "photo-1",
                bytes = bytes,
                width = 2,
                height = 1,
                marketplaceId = "market-a",
            )

            assertTrue(persisted)
            assertEquals(output, stored)
            assertEquals("photo-1", output.itemId)
            assertEquals(2, output.width)
            assertEquals(1, output.height)
            assertEquals(4L, output.bytes)
            assertEquals("png", output.format)
            assertEquals(Provenance.ENHANCED, output.provenance)
            assertEquals(SlotRole.EXTRA, output.role)
            assertTrue(output.operationsJson.contains("\"operation\":\"auto-card\""))
            val file = File(output.path)
            assertTrue(file.isFile)
            assertArrayEquals(bytes, file.readBytes())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun removesOutputFileWhenRoomPersistenceFails() = runBlocking {
        val directory = File("build/tmp/project-output-${UUID.randomUUID()}")
        try {
            val writer = ProjectOutputWriter(directory) {
                throw IllegalStateException("Room write failed")
            }

            try {
                writer.saveAiCard("photo-1", byteArrayOf(1), 1, 1, null)
                throw AssertionError("a failed Room write must be reported")
            } catch (expected: IllegalStateException) {
                assertEquals("Room write failed", expected.message)
            }

            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }
}

package com.gexetr.coverkit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExportRendererTest {

    @Test
    fun ytThumbBitmapIsExactCatalogPixels() {
        val project = TemplateFactory.createProject(
            templateId = "yt_thumb_bold",
            projectId = "test-yt",
            nowMs = 1L,
        )
        val bitmap = ExportRenderer.render(project, watermark = false)
        try {
            assertEquals(1280, bitmap.width)
            assertEquals(720, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun igPostBitmapIsExactCatalogPixels() {
        val project = TemplateFactory.createProject(
            templateId = "ig_post_square",
            projectId = "test-ig",
            nowMs = 1L,
        )
        val bitmap = ExportRenderer.render(project, watermark = false)
        try {
            assertEquals(1080, bitmap.width)
            assertEquals(1080, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun watermarkPathDoesNotCrashAndKeepsDimensions() {
        val project = TemplateFactory.blankProject(
            destinationId = "yt_thumb",
            projectId = "wm",
            nowMs = 1L,
        )
        val bitmap = ExportRenderer.render(project, watermark = true)
        try {
            assertEquals(1280, bitmap.width)
            assertEquals(720, bitmap.height)
            val png = ExportRenderer.renderPng(project, watermark = true)
            assertTrue(png.isNotEmpty())
            // PNG signature
            assertEquals(0x89.toByte(), png[0])
            assertEquals('P'.code.toByte(), png[1])
            assertEquals('N'.code.toByte(), png[2])
            assertEquals('G'.code.toByte(), png[3])
        } finally {
            bitmap.recycle()
        }
    }
}

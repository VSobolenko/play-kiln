package com.gexetr.plinthproductphotos.data.export

import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Resolves the share provider through the real merged manifest, as ExportDelivery does. */
@RunWith(AndroidJUnit4::class)
class FileProviderManifestTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val authority = "${context.packageName}.fileprovider"

    @Test
    fun mergedManifestDeclaresUnexportedGrantingProvider() {
        val info = context.packageManager.resolveContentProvider(authority, PackageManager.GET_META_DATA)
        assertNotNull("no provider for $authority in the merged manifest", info)
        assertEquals(FileProvider::class.java.name, info!!.name)
        assertFalse(info.exported)
        assertTrue(info.grantUriPermissions)
    }

    @Test
    fun exportsFileResolvesToContentUri() {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "manifest-test.jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            val uri = FileProvider.getUriForFile(context, authority, file)
            assertEquals("content", uri.scheme)
            assertEquals(authority, uri.authority)
        } finally {
            file.delete()
        }
    }

    @Test
    fun cacheOutsideExportsIsNotShareable() {
        val file = File(context.cacheDir, "outside-exports.jpg").apply { writeBytes(byteArrayOf(1)) }
        try {
            FileProvider.getUriForFile(context, authority, file)
            fail("a cache file outside exports/ must not be shareable")
        } catch (expected: IllegalArgumentException) {
            // the path scope is confined to cache/exports/
        } finally {
            file.delete()
        }
    }
}

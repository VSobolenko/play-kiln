package com.gexetr.plinthproductphotos.data.export

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gexetr.plinthproductphotos.domain.export.ExportArtifact
import com.gexetr.plinthproductphotos.domain.export.ExportPack
import com.gexetr.plinthproductphotos.domain.export.MarketplaceBundleWriter
import com.gexetr.plinthproductphotos.domain.export.ZipPackWriter
import java.io.File
import java.io.IOException

object ExportDelivery {
    fun prepareShareUri(context: Context, artifact: ExportArtifact): Uri {
        return prepareShareUri(context, artifact.fileName, artifact.bytes)
    }

    fun prepareShareUri(context: Context, fileName: String, bytes: ByteArray): Uri {
        val directory = File(context.cacheDir, "exports")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create the temporary export directory")
        }
        val file = File.createTempFile("plinth-share-", "-$fileName", directory)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun shareImage(context: Context, artifact: ExportArtifact, uri: Uri, chooserTitle: String) {
        shareImage(context, artifact.fileName, artifact.mimeType(), uri, chooserTitle)
    }

    fun shareImage(context: Context, fileName: String, mimeType: String, uri: Uri, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
        if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveImageToGallery(context: Context, artifact: ExportArtifact): Uri {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Gallery saving requires Android 10 or later; use the file picker on this device"
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, artifact.fileName)
            put(MediaStore.Images.Media.MIME_TYPE, artifact.mimeType())
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Plinth")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Gallery did not create an image entry")
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { it.write(artifact.bytes) }
                ?: throw IOException("Gallery image stream is unavailable")
            val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            check(context.contentResolver.update(uri, published, null, null) == 1) {
                "Gallery did not publish the image"
            }
            return uri
        } catch (failure: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw failure
        }
    }

    fun writeImageTo(context: Context, uri: Uri, artifact: ExportArtifact) {
        writeBytesTo(context, uri, artifact.bytes)
    }

    fun writeBytesTo(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
            ?: throw IOException("The selected file cannot be written")
    }

    fun writePackTo(context: Context, uri: Uri, pack: ExportPack) {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("The selected ZIP destination cannot be written")
        output.use { ZipPackWriter.write(pack, it) }
    }

    fun writeBundleTo(context: Context, uri: Uri, packs: List<ExportPack>) {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("The selected ZIP destination cannot be written")
        output.use { MarketplaceBundleWriter.write(packs, it) }
    }

    private fun ExportArtifact.mimeType(): String = when (format) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> throw IllegalArgumentException("Unsupported image format: $format")
    }
}

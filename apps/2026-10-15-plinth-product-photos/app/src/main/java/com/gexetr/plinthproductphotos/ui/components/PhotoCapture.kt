package com.gexetr.plinthproductphotos.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class PhotoCaptureLauncher(
    private val context: Context,
    private val onCaptured: (Uri) -> Unit,
    private val onCancelled: () -> Unit,
    private val onDenied: () -> Unit,
) {
    private var takePicture: ((Uri) -> Unit)? = null
    private var requestPermission: (() -> Unit)? = null
    private var pendingUri: Uri? = null

    @Composable
    fun Bind() {
        val pictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
            val uri = pendingUri
            pendingUri = null
            uri?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            if (captured && uri != null) onCaptured(uri) else onCancelled()
        }
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCapture() else onDenied()
        }
        takePicture = pictureLauncher::launch
        requestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    fun launch() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCapture()
        } else {
            requestPermission?.invoke() ?: onDenied()
        }
    }

    private fun launchCapture() {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "photo-${UUID.randomUUID()}.jpg")
        if (!runCatching { file.createNewFile() }.getOrDefault(false)) {
            onCancelled()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.packageManager.queryIntentActivities(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), 0)
            .forEach { handler ->
                context.grantUriPermission(
                    handler.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        pendingUri = uri
        takePicture?.invoke(requireNotNull(pendingUri)) ?: onCancelled()
    }
}

@Composable
fun rememberPhotoCaptureLauncher(
    onCaptured: (Uri) -> Unit,
    onCancelled: () -> Unit,
    onDenied: () -> Unit,
): PhotoCaptureLauncher {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = remember(context, onCaptured, onCancelled, onDenied) {
        PhotoCaptureLauncher(context, onCaptured, onCancelled, onDenied)
    }
    launcher.Bind()
    return launcher
}

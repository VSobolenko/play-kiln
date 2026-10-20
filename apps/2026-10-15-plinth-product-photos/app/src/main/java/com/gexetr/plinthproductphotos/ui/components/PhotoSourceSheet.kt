package com.gexetr.plinthproductphotos.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.domain.wallet.WalletAccessPolicy
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

enum class PhotoSource { CAMERA, GALLERY, FILES }

/**
 * The one place a photo enters the app. Three doors, each named in the user's language and each row
 * carrying the slot's own accent rather than a stock platform glyph — the sheet is the first thing a
 * new user touches, so it is styled like the rest of the studio and not like a system dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceSheet(
    onDismiss: () -> Unit,
    onSource: (PhotoSource) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.photo_source_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            PhotoSourceRow(
                source = PhotoSource.CAMERA,
                title = R.string.photo_source_camera,
                description = R.string.photo_source_camera_description,
                icon = R.drawable.ic_lucide_camera,
                accent = PlinthPalette.Spark,
                onSource = onSource,
            )
            PhotoSourceRow(
                source = PhotoSource.GALLERY,
                title = R.string.photo_source_gallery,
                description = R.string.photo_source_gallery_description,
                icon = R.drawable.ic_lucide_image,
                accent = PlinthPalette.Amber,
                onSource = onSource,
            )
            PhotoSourceRow(
                source = PhotoSource.FILES,
                title = R.string.photo_source_files,
                description = R.string.photo_source_files_description,
                icon = R.drawable.ic_lucide_file,
                accent = PlinthPalette.Glow,
                onSource = onSource,
            )
        }
    }
}

@Composable
private fun PhotoSourceRow(
    source: PhotoSource,
    title: Int,
    description: Int,
    icon: Int,
    accent: androidx.compose.ui.graphics.Color,
    onSource: (PhotoSource) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSource(source) }
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = .14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                // The row's own title announces the action, so the glyph is decorative here.
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Selection policy for import sources: the camera always produces one fresh URI. */
fun <T> importSourceSelection(
    source: PhotoSource,
    uris: List<T>,
    serverConfirmedUnlock: Boolean,
): List<T> = when (source) {
    PhotoSource.CAMERA -> uris.take(1)
    PhotoSource.GALLERY, PhotoSource.FILES -> WalletAccessPolicy.clampSelection(uris, serverConfirmedUnlock)
}

/**
 * Renders the sheet and returns the action a single-photo entry point calls.
 *
 * The caller receives a URI that is already picked, so it must hand that URI to the import path. A
 * caller that ignores the argument makes the sheet a dead end — the user picks a photo and nothing
 * happens — which is the defect this contract exists to name.
 */
@Composable
fun rememberPhotoSourceFlow(onPhoto: (Uri) -> Unit): () -> Unit {
    var showSheet by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPhoto)
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPhoto)
    }
    val camera = rememberPhotoCaptureLauncher(
        onCaptured = onPhoto,
        onCancelled = {},
        onDenied = { showSheet = true },
    )
    if (showSheet) {
        PhotoSourceSheet(
            onDismiss = { showSheet = false },
            onSource = { source ->
                showSheet = false
                when (source) {
                    PhotoSource.CAMERA -> camera.launch()
                    PhotoSource.GALLERY -> galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    PhotoSource.FILES -> filePicker.launch(arrayOf("image/*"))
                }
            },
        )
    }
    return { showSheet = true }
}

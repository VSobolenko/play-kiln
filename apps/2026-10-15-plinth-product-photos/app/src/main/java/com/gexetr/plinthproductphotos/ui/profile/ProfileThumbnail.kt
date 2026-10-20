package com.gexetr.plinthproductphotos.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberProfileThumbnail(path: String): State<Bitmap?> = produceState<Bitmap?>(
    initialValue = null,
    key1 = path,
) {
    value = withContext(Dispatchers.IO) { decodeProfileThumbnail(path) }
}

internal fun decodeProfileThumbnail(path: String, maxSidePx: Int = PROFILE_THUMBNAIL_MAX_SIDE_PX): Bitmap? {
    if (path.isBlank() || maxSidePx <= 0) return null
    return runCatching {
        val file = File(path)
        if (!file.isFile || !file.canRead()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }.getOrNull()
}

private fun sampleSize(width: Int, height: Int, maxSidePx: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= maxSidePx || height / (sample * 2) >= maxSidePx) sample *= 2
    return sample
}

private const val PROFILE_THUMBNAIL_MAX_SIDE_PX = 256

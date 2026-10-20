package com.gexetr.plinthproductphotos.ui.importscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.gexetr.plinthproductphotos.R

@Composable
internal fun SelectedMediaPreview(
    model: Any?,
    name: String,
    state: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.2f,
    isPreparing: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = stringResource(R.string.plinth_recent_photo_content_description, name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        if (isPreparing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                stringResource(R.string.w3_preview_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                state,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Only the photo owns this gesture; horizontal choosers and text editing keep theirs.
internal fun Modifier.photoSwipeBack(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    val threshold = 96.dp.toPx()
    var distance = 0f
    detectHorizontalDragGestures(
        onDragStart = { distance = 0f },
        onDragCancel = { distance = 0f },
        onDragEnd = {
            if (distance <= -threshold) onBack()
            distance = 0f
        },
        onHorizontalDrag = { change, amount ->
            change.consume()
            distance += amount
        },
    )
}

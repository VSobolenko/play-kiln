package com.gexetr.plinthproductphotos.ui.autocard

import android.graphics.Bitmap
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun FormingCardPreview(
    bitmap: Bitmap?,
    aspectRatio: Float,
    busy: Boolean,
    message: String,
    failed: Boolean,
    modifier: Modifier = Modifier,
) {
    val ready = bitmap != null && !busy && !failed
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val motionEnabled = rememberMotionEnabled()
    var formingMessage by remember { mutableStateOf(message) }
    LaunchedEffect(ready, message) {
        if (!ready) formingMessage = message
    }
    val skeletonAlpha by animateFloatAsState(
        targetValue = if (ready) 0f else 1f,
        animationSpec = if (motionEnabled) tween(320, easing = FastOutSlowInEasing) else snap(),
        label = "forming-card-fade",
    )
    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        // Keep the delivered bitmap composed while busy/failed changes. Replacing this subtree
        // made the result flash blank during apply/save and forced a fresh image allocation.
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.autocard_preview_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1f - skeletonAlpha
                    scaleX = 1f - 0.015f * skeletonAlpha
                    scaleY = scaleX
                },
            )
        }
        if (skeletonAlpha > 0f) {
            FormingSkeleton(
                animated = busy && !failed && motionEnabled,
                alpha = skeletonAlpha,
                message = if (ready) formingMessage else message,
                failed = failed,
            )
        }
    }
}

@Composable
private fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.applicationContext.contentResolver
    var durationScale by remember(resolver) { mutableFloatStateOf(readAnimatorDurationScale(resolver)) }
    DisposableEffect(resolver) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                durationScale = readAnimatorDurationScale(resolver)
            }
        }
        resolver.registerContentObserver(uri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return durationScale > 0f
}

private fun readAnimatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

@Composable
private fun FormingSkeleton(animated: Boolean, alpha: Float, message: String, failed: Boolean) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val silhouette = MaterialTheme.colorScheme.secondaryContainer
    val accent = MaterialTheme.colorScheme.primaryContainer
    val horizon = MaterialTheme.colorScheme.outlineVariant
    val sweep = if (animated) {
        val transition = rememberInfiniteTransition(label = "forming-card")
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
            label = "forming-card-sweep",
        )
    } else {
        rememberUpdatedState(0f)
    }
    // Keep the mesh geometry fixed. A second infinite animation invalidated every gradient and
    // silhouette on each frame while the surrounding LazyColumn was also handling scroll.
    val morph = 0.35f
    Box(
        modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.drawWithCache {
            val aspect = size.width / size.height.coerceAtLeast(1f)
            val portraitWeight = ((1.15f - aspect) / 0.45f).coerceIn(0f, 1f)
            val phase = morph * (2f * PI.toFloat())
            val driftX = sin(phase.toDouble()).toFloat() * size.width * 0.035f
            val driftY = cos(phase.toDouble()).toFloat() * size.height * 0.025f
            val productWidth = size.width * (0.36f + portraitWeight * 0.14f) * (0.96f + morph * 0.06f)
            val productHeight = size.height * (0.43f - portraitWeight * 0.08f) * (1.03f - morph * 0.05f)
            val productTop = Offset(
                x = (size.width - productWidth) / 2f + driftX,
                y = size.height * (0.16f + portraitWeight * 0.04f) + driftY,
            )
            val radius = CornerRadius(size.minDimension * (0.07f + portraitWeight * 0.025f))
            val meshRadius = size.maxDimension * (0.58f + portraitWeight * 0.12f)
            val horizonY = size.height * (0.60f + portraitWeight * 0.04f)
            onDrawBehind {
                drawRect(base)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.56f), Color.Transparent),
                        center = Offset(size.width * 0.18f + driftX, size.height * 0.15f),
                        radius = meshRadius,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(highlight.copy(alpha = 0.78f), Color.Transparent),
                        center = Offset(size.width * 0.82f - driftX, size.height * 0.42f + driftY),
                        radius = meshRadius * 0.82f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(silhouette.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(size.width * 0.48f, size.height * (0.92f - portraitWeight * 0.08f)),
                        radius = meshRadius * 0.7f,
                    ),
                )
                drawLine(
                    color = horizon,
                    start = Offset(size.width * 0.1f, horizonY),
                    end = Offset(size.width * 0.9f, horizonY),
                    strokeWidth = 1.dp.toPx(),
                )
                drawOval(
                    color = horizon.copy(alpha = 0.35f),
                    topLeft = Offset(size.width * 0.2f, horizonY - size.height * 0.035f),
                    size = Size(size.width * 0.6f, size.height * (0.07f + portraitWeight * 0.025f)),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(silhouette, highlight, accent, silhouette),
                        start = Offset(size.width * sweep.value, 0f),
                        end = Offset(size.width * (sweep.value + 1f), size.height),
                    ),
                    topLeft = productTop,
                    size = Size(productWidth, productHeight),
                    cornerRadius = radius,
                )
                if (animated) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                highlight.copy(alpha = 0.34f),
                                Color.Transparent,
                            ),
                            start = Offset(size.width * (sweep.value - 0.55f), 0f),
                            end = Offset(size.width * (sweep.value + 0.25f), size.height),
                        ),
                    )
                }
            }
        },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().background(base).padding(24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

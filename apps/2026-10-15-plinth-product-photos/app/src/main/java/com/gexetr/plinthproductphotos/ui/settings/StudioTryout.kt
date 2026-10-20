package com.gexetr.plinthproductphotos.ui.settings

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.data.StudioDefaults
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels
import com.gexetr.plinthproductphotos.data.photoimport.PhotoPixels.toBitmap
import com.gexetr.plinthproductphotos.data.project.ProjectRepository
import com.gexetr.plinthproductphotos.ui.components.PlinthStage
import com.gexetr.plinthproductphotos.ui.theme.PlinthProductPalette
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
internal fun StudioPreview(state: StudioDefaults, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        try {
            photo = withContext(Dispatchers.IO) {
                val repository = ProjectRepository(context)
                repository.recentPhotos().firstOrNull()?.let { recent ->
                    val pixels = PhotoPixels.load(repository.sourceFile(recent.sourcePath))
                    val ratio = 320f / maxOf(pixels.width, pixels.height)
                    pixels.scale(
                        (pixels.width * ratio).toInt().coerceAtLeast(1),
                        (pixels.height * ratio).toInt().coerceAtLeast(1),
                    ).toBitmap()
                }
            }
        } catch (failure: IOException) {
            Log.e("SettingsStudio", "Could not load preview photo", failure)
            failed = true
        } catch (failure: IllegalArgumentException) {
            Log.e("SettingsStudio", "Invalid preview photo", failure)
            failed = true
        } catch (failure: JSONException) {
            Log.e("SettingsStudio", "Invalid preview library record", failure)
            failed = true
        } finally {
            loading = false
        }
    }
    val description = stringResource(R.string.plinth_settings_preview_description)
    val background = when (state.background) {
        1 -> PlinthProductPalette.Graphite
        2 -> PlinthProductPalette.Travertine
        else -> PlinthProductPalette.Paper
    }
    val fraction = minOf(1f - 2f * state.marginPct, state.fillTarget * state.itemScale)
    val status = when {
        loading -> R.string.plinth_settings_preview_loading
        failed -> R.string.plinth_settings_preview_error
        photo == null -> R.string.plinth_settings_preview_sample
        else -> R.string.plinth_settings_preview_photo
    }
    PlinthStage(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(background)
                    .semantics { contentDescription = description },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val margin = state.marginPct * size.minDimension
                    drawRect(
                        color = PlinthPalette.BezelRim.copy(alpha = 0.5f),
                        topLeft = Offset(margin, margin),
                        size = Size(size.width - 2 * margin, size.height - 2 * margin),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    if (state.shadow != 0) {
                        val centerX = size.width * (0.5f + state.horizontalPosition)
                        val bottom = size.height * (0.5f + state.verticalPosition + fraction * 0.46f)
                        for (step in 6 downTo 1) {
                            val spread = step * size.width * 0.008f
                            drawOval(
                                color = PlinthPalette.Ink.copy(alpha = 0.035f),
                                topLeft = Offset(
                                    centerX - size.width * fraction * 0.27f - spread,
                                    bottom - spread + if (state.shadow == 2) size.height * 0.04f else 0f,
                                ),
                                size = Size(
                                    size.width * fraction * 0.54f + spread * 2,
                                    size.height * fraction * 0.06f + spread * 2,
                                ),
                            )
                        }
                    }
                }
                val itemModifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = fraction
                    scaleY = fraction
                    translationX = state.horizontalPosition * size.width
                    translationY = state.verticalPosition * size.height
                }
                val bitmap = photo
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = itemModifier,
                    )
                } else {
                    SampleVessel(itemModifier)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.plinth_settings_live),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SampleVessel(modifier: Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 100f, size.height / 100f, Offset.Zero) }) {
            drawRoundRect(
                color = PlinthProductPalette.Clay,
                topLeft = Offset(23f, 25f),
                size = Size(54f, 70f),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawRoundRect(
                color = PlinthProductPalette.Travertine,
                topLeft = Offset(34f, 7f),
                size = Size(32f, 27f),
                cornerRadius = CornerRadius(5f, 5f),
            )
            drawOval(
                color = PlinthProductPalette.ClayDeep,
                topLeft = Offset(35f, 4f),
                size = Size(30f, 8f),
            )
            drawRoundRect(
                color = PlinthProductPalette.Paper.copy(alpha = 0.25f),
                topLeft = Offset(30f, 37f),
                size = Size(7f, 43f),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}

@Composable
internal fun StudioControls(
    state: StudioDefaults,
    onChange: ((StudioDefaults) -> StudioDefaults) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioChoices(
            stringResource(R.string.plinth_background_section),
            stringResource(R.string.plinth_settings_background_note),
            listOf(
                stringResource(R.string.plinth_settings_paper),
                stringResource(R.string.plinth_settings_graphite),
                stringResource(R.string.plinth_settings_travertine),
            ),
            state.background,
        ) { value -> onChange { it.copy(background = value) } }
        StudioChoices(
            stringResource(R.string.plinth_shadow_section),
            stringResource(R.string.plinth_settings_shadow_note),
            listOf(
                stringResource(R.string.plinth_shadow_none),
                stringResource(R.string.plinth_shadow_contact),
                stringResource(R.string.plinth_shadow_drop),
            ),
            state.shadow,
        ) { value -> onChange { it.copy(shadow = value) } }
        StudioSlider(
            stringResource(R.string.plinth_fill_value, (state.fillTarget * 100).toInt()),
            stringResource(R.string.plinth_settings_fill_note),
            state.fillTarget, 0.55f..0.95f,
        ) { value -> onChange { it.copy(fillTarget = value) } }
        StudioSlider(
            stringResource(R.string.plinth_margin_value, (state.marginPct * 100).toInt()),
            stringResource(R.string.plinth_settings_margin_note),
            state.marginPct, 0f..0.14f,
        ) { value -> onChange { it.copy(marginPct = value) } }
        StudioSlider(
            stringResource(R.string.plinth_item_scale_value, (state.itemScale * 100).toInt()),
            stringResource(R.string.plinth_settings_scale_note),
            state.itemScale, 0.75f..1.25f,
        ) { value -> onChange { it.copy(itemScale = value) } }
        StudioSlider(
            stringResource(R.string.plinth_horizontal_value, (state.horizontalPosition * 100).toInt()),
            stringResource(R.string.plinth_settings_horizontal_note),
            state.horizontalPosition, -0.12f..0.12f,
        ) { value -> onChange { it.copy(horizontalPosition = value) } }
        StudioSlider(
            stringResource(R.string.plinth_vertical_value, (state.verticalPosition * 100).toInt()),
            stringResource(R.string.plinth_settings_vertical_note),
            state.verticalPosition, -0.12f..0.12f,
        ) { value -> onChange { it.copy(verticalPosition = value) } }
        TextButton(onClick = { onChange { StudioDefaults() } }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.plinth_settings_reset))
        }
    }
}

@Composable
private fun StudioChoices(
    title: String,
    note: String,
    choices: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        choices.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun StudioSlider(
    title: String,
    note: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { contentDescription = title },
        )
    }
}

@Composable
internal fun DeliveryNotes() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.plinth_settings_delivery_note), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.plinth_settings_delivery_proof),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

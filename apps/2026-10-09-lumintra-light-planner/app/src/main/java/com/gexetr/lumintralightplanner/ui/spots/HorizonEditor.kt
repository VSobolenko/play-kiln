package com.gexetr.lumintralightplanner.ui.spots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.ui.format.formatDegrees
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.PlayKilnTheme
import kotlin.math.roundToInt

// The horizon editor: a fixed 360-sample array (domain/HorizonProfile.kt) shown as a filled
// silhouette against an azimuth axis, edited by dragging a finger across it.
//
// Two properties are load-bearing and are honoured rather than approximated:
//
// 1. The ARRAY is the truth and the canvas is only a view of it. Every gesture writes through
//    HorizonProfile.withSample, which copies the 360 floats, clamps to [-10, +90] and stores tenths
//    of a degree. Nothing here keeps a bitmap whose meaning depends on the screen resolution, so the
//    same profile drawn on a phone reads identically on a tablet (specs/scout-spots).
// 2. The azimuth axis is GEOGRAPHIC and is NEVER mirrored by layout direction. 0 deg (north) is at
//    the left edge and 360 deg at the right, always. The app is `supportsRtl="true"`, so the axis is
//    explicitly wrapped in LocalLayoutDirection = Ltr; without that a right-to-left locale would put
//    the user's west ridge on the wrong side and make the drawing a lie.
//
// The declared vertical field is adjustable (default 0..30 deg); the axis is drawn from -10 deg (the
// storable floor) up to the chosen field, so a below-horizon sample is visible rather than clipped
// into a flat edge. There is no horizontal pan or zoom, so the visible azimuth range is always the
// whole 0..360 deg axis and the 0 deg point cannot be shifted.

/** The vertical fields the editor offers. -10 deg is always the axis floor; +90 deg is the cap. */
internal val HORIZON_FIELD_OPTIONS: List<Int> = listOf(15, 30, 45, 90)

private val EDITOR_HEIGHT = 220.dp

/** The azimuth axis labels: north at the left, increasing clockwise to the right. */
private val AZIMUTH_LABELS: List<Int> = listOf(
    R.string.coord_hemisphere_n,
    R.string.coord_hemisphere_e,
    R.string.coord_hemisphere_s,
    R.string.coord_hemisphere_w,
    R.string.coord_hemisphere_n,
)

/**
 * Draws and edits a [HorizonProfile].
 *
 * [profile] is `null` when the spot has never had a horizon drawn: the canvas then shows the flat
 * fallback at 0 deg and the caller renders the "no horizon profile" sentence. The first sample the
 * user writes creates a real profile, so the two states are never collapsed into one.
 */
@Composable
fun HorizonEditor(
    profile: HorizonProfile?,
    fieldDeg: Int,
    onSample: (azimuthIndex: Int, elevationDeg: Double) -> Unit,
    onReset: () -> Unit,
    onFieldChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lumintra = LumintraTheme.colors
    val axisMin = HorizonProfile.MIN_DEG
    val field = HORIZON_FIELD_OPTIONS.firstOrNull { it == fieldDeg } ?: HORIZON_FIELD_OPTIONS[1]
    val axisMax = field.toDouble()

    // The canvas reads this directly: O(1) per sample, no per-sample object allocation.
    val flat = remember { FloatArray(HorizonProfile.SAMPLE_COUNT) }
    val samples = profile?.altitudeDeg ?: flat

    var selectedAzimuth by rememberSaveable { mutableStateOf(0) }
    val currentOnSample by rememberUpdatedState(onSample)
    val canvasDescription = stringResource(R.string.spot_edit_horizon_description)
    val azimuthSliderDescription = stringResource(R.string.spot_edit_horizon_select_azimuth_description)
    val elevationSliderDescription = stringResource(R.string.spot_edit_horizon_set_elevation_description)
    val selectedIndex = selectedAzimuth.coerceIn(0, HorizonProfile.SAMPLE_COUNT - 1)
    val selectedElevation = samples[selectedIndex].toDouble()

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.spot_edit_horizon_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The declared vertical field, adjustable. A control rather than a gesture, so it stays
        // reachable by TalkBack.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.spot_edit_horizon_field_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (option in HORIZON_FIELD_OPTIONS) {
                FieldChip(
                    label = stringResource(R.string.spot_edit_horizon_field_value, option),
                    selected = option == field,
                    onSelect = { onFieldChange(option) },
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(lumintra.skyZenith, MaterialTheme.shapes.medium),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EDITOR_HEIGHT)
                        .semantics { contentDescription = canvasDescription }
                        .pointerInput(field) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                writeSample(
                                    position = down.position,
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat(),
                                    axisMin = axisMin,
                                    axisMax = axisMax,
                                    onSample = currentOnSample,
                                )
                                down.consume()
                                drag(pointerId = down.id) { change ->
                                    writeSample(
                                        position = change.position,
                                        width = size.width.toFloat(),
                                        height = size.height.toFloat(),
                                        axisMin = axisMin,
                                        axisMax = axisMax,
                                        onSample = currentOnSample,
                                    )
                                    change.consume()
                                }
                            }
                        },
                ) {
                    val span = (axisMax - axisMin).coerceAtLeast(1.0)
                    fun yFor(degrees: Double): Float = ((axisMax - degrees) / span).toFloat() * size.height
                    fun xFor(index: Int): Float = (index.toFloat() / HorizonProfile.SAMPLE_COUNT) * size.width

                    // Degree grid: every 10 deg of elevation, every 30 deg of azimuth.
                    var elevation = axisMin
                    while (elevation <= axisMax) {
                        val y = yFor(elevation)
                        drawLine(
                            color = lumintra.hairline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                        elevation += 10.0
                    }
                    for (azimuth in 0..360 step 30) {
                        val x = xFor(azimuth)
                        drawLine(
                            color = lumintra.hairline,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                        )
                    }

                    val baseline = yFor(axisMin)
                    val ground = Path().apply {
                        moveTo(0f, baseline)
                        for (index in 0 until HorizonProfile.SAMPLE_COUNT) {
                            lineTo(xFor(index), yFor(samples[index].toDouble()))
                        }
                        // Azimuth 360 is azimuth 0 on the other side of the axis, so the right edge
                        // closes onto the first sample instead of jumping.
                        lineTo(size.width, yFor(samples[0].toDouble()))
                        lineTo(size.width, baseline)
                        close()
                    }
                    drawPath(path = ground, color = lumintra.skyHorizon)
                    drawPath(path = ground, color = lumintra.firstLight, style = Stroke(width = 2f))

                    // The sample the numeric slider edits, marked so the two editors agree.
                    val x = xFor(selectedIndex)
                    drawLine(
                        color = lumintra.onSkyMuted,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                    drawCircle(
                        color = lumintra.firstLight,
                        radius = 4f,
                        center = Offset(x, yFor(selectedElevation)),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (key in AZIMUTH_LABELS) {
                        Text(
                            text = stringResource(key),
                            style = MaterialTheme.typography.labelSmall,
                            color = lumintra.onSkyMuted,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(
                R.string.spot_edit_horizon_axis_range,
                HorizonProfile.MIN_DEG.roundToInt(),
                field,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        // The numeric path: a user who cannot perform a free-hand drag authors the same array here,
        // and it is stored identically.
        Text(
            text = stringResource(R.string.spot_edit_horizon_selected_azimuth, selectedIndex),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { selectedAzimuth = it.roundToInt().coerceIn(0, HorizonProfile.SAMPLE_COUNT - 1) },
            valueRange = 0f..(HorizonProfile.SAMPLE_COUNT - 1).toFloat(),
            steps = HorizonProfile.SAMPLE_COUNT - 2,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = azimuthSliderDescription },
        )

        Text(
            text = stringResource(R.string.spot_edit_horizon_elevation_value, formatDegrees(selectedElevation)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = selectedElevation.toFloat().coerceIn(axisMin.toFloat(), axisMax.toFloat()),
            onValueChange = { onSample(selectedIndex, it.toDouble()) },
            valueRange = axisMin.toFloat()..axisMax.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = elevationSliderDescription },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = { onSample(selectedIndex, selectedElevation - 1.0) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.spot_edit_horizon_step_down))
            }
            FilledTonalButton(
                onClick = { onSample(selectedIndex, selectedElevation + 1.0) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.spot_edit_horizon_step_up))
            }
        }

        if (profile == null) {
            Text(
                text = stringResource(R.string.spot_edit_horizon_no_profile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (profile.lowestDeg >= HorizonProfile.MAX_DEG) {
            // A sky-blocking profile is legal and is stored. It is usually a mis-draw, so it gets one
            // non-blocking confirmation and the reset affordance, never a dialog.
            Text(
                text = stringResource(R.string.spot_edit_horizon_full_sky_note),
                style = MaterialTheme.typography.bodySmall,
                color = lumintra.caution,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        FilledTonalButton(
            onClick = onReset,
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.spot_edit_horizon_reset))
        }
    }
}

/**
 * Maps a touch to one whole-degree sample and writes it.
 *
 * The azimuth maps linearly across the full width (index = floor(x / width * 360)), so nothing in the
 * view can change what the profile means: there is no view transform to shift the 0 deg point, and
 * the visible range is the whole 0..360 deg axis.
 */
private fun writeSample(
    position: Offset,
    width: Float,
    height: Float,
    axisMin: Double,
    axisMax: Double,
    onSample: (Int, Double) -> Unit,
) {
    if (width <= 0f || height <= 0f) return
    val xFraction = (position.x / width).coerceIn(0f, 0.9999f)
    val index = (xFraction * HorizonProfile.SAMPLE_COUNT).toInt()
        .coerceIn(0, HorizonProfile.SAMPLE_COUNT - 1)
    val yFraction = (position.y / height).coerceIn(0f, 1f)
    val elevation = axisMax - yFraction * (axisMax - axisMin)
    onSample(index, elevation)
}

/** One vertical-field choice. Selection is carried by a border and by weight, never by colour alone. */
@Composable
private fun RowScope.FieldChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .weight(1f)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = colors.onSurface,
        )
    }
}

@Preview(name = "Horizon editor", showBackground = true, widthDp = 412)
@Composable
private fun HorizonEditorPreview() {
    PlayKilnTheme(darkTheme = true, dynamicColor = false) {
        HorizonEditor(
            profile = HorizonProfile.flat(
                id = "preview",
                name = "preview",
                latitudeDeg = 59.913,
                longitudeDeg = 10.752,
                elevationDeg = 4.0,
            ),
            fieldDeg = 30,
            onSample = { _, _ -> },
            onReset = {},
            onFieldChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

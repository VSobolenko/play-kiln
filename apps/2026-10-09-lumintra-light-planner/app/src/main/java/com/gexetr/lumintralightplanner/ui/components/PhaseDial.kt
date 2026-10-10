package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import kotlin.math.abs

/**
 * The Moon as it will actually be tonight: a real disc with a lit limb and an elliptical terminator,
 * drawn from the illuminated fraction instead of from one of eight stock pictures.
 *
 * @param fraction the Moon's **illuminated fraction**, in `[0.0, 1.0]`, **signed for the side**:
 *   pass it positive while the Moon is **waxing** (after new, before full) so the lit limb is drawn on
 *   the **right**, and negative while it is **waning** so the lit limb is drawn on the **left**. The
 *   illuminated fraction is symmetrical about the quarters — a first and a last quarter are both
 *   `0.5` — so the side is the one bit it cannot carry, and the sign is where it is written. A
 *   `MoonState` supplies it as
 *   `if (state.elongationDeg < 180.0) state.illuminatedFraction else -state.illuminatedFraction`.
 *   Passing an unsigned illumination draws an astronomically correct *shape* lit from the right, which
 *   is correct for every waxing night and a mirror of every waning one.
 * @param phaseName the phase's name, already localised by the caller, drawn under the disc.
 * @param size the diameter of the disc. The strip under it is laid out by its own text, so the
 *   component as a whole can grow with the font scale while the disc stays the size asked for.
 *
 * The geometry is the standard projection of the terminator: the boundary between the lit and unlit
 * hemispheres projects onto the disc as an **ellipse** whose horizontal semi-axis is
 * `|1 − 2k| · r` for an illuminated fraction `k`, and whose vertical semi-axis is the disc's own
 * radius. A crescent bows that ellipse toward the lit limb, a gibbous moon bows it past the centre to
 * the other side, and the two quarters degenerate it to the vertical diameter — which is why the
 * drawing branches on the sign of `1 − 2k` rather than interpolating a picture.
 *
 * The disc is filled in [com.gexetr.lumintralightplanner.ui.theme.LumintraColors.moonShadow] beneath
 * the lit path, so an unlit limb is visible as a body rather than as a hole, and the whole disc
 * carries a hairline rim — the same rule the rest of the identity is drawn with.
 */
@Composable
fun PhaseDial(
    fraction: Double,
    phaseName: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val colours = LumintraTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val box = this.size
            val shortest = box.minDimension
            if (shortest <= 0f) return@Canvas

            val centre = Offset(box.width / 2f, box.height / 2f)
            val radius = shortest / 2f * 0.86f
            val rimWidth = 1.dp.toPx()

            // The Moon's own light on the night around it, so the disc sits in the sky rather than on it.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colours.moonlight.copy(alpha = 0.18f), Color.Transparent),
                    center = centre,
                    radius = radius * 1.16f,
                ),
                radius = radius * 1.16f,
                center = centre,
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colours.moonShadow,
                        lerp(colours.moonShadow, colours.hairline, 0.25f),
                    ),
                    center = centre + Offset(-radius * 0.12f, radius * 0.08f),
                    radius = radius * 1.05f,
                ),
                radius = radius,
                center = centre,
            )

            val lit = abs(fraction).coerceIn(0.0, 1.0)
            val waxing = fraction >= 0.0
            val litHighlight = lerp(colours.moonLit, colours.firstLight, 0.18f)
            when {
                lit >= FULL -> drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(litHighlight, colours.moonLit),
                        center = centre + Offset(radius * 0.22f, -radius * 0.18f),
                        radius = radius * 1.1f,
                    ),
                    radius = radius,
                    center = centre,
                )

                lit <= NEW -> Unit

                else -> {
                    val limb = Rect(
                        left = centre.x - radius,
                        top = centre.y - radius,
                        right = centre.x + radius,
                        bottom = centre.y + radius,
                    )
                    // Signed: positive while the terminator is a crescent bulging toward the lit limb.
                    val terminator = (1.0 - 2.0 * lit).toFloat()
                    val bow = abs(terminator) * radius
                    val path = Path()
                    path.moveTo(centre.x, centre.y - radius)
                    // The lit limb: the half-circle on the lit side, top to bottom.
                    path.arcTo(limb, 270f, if (waxing) 180f else -180f, false)
                    if (bow < radius * 0.01f) {
                        // A quarter: the terminator is the vertical diameter.
                        path.lineTo(centre.x, centre.y - radius)
                    } else {
                        val terminatorBounds = Rect(
                            left = centre.x - bow,
                            top = centre.y - radius,
                            right = centre.x + bow,
                            bottom = centre.y + radius,
                        )
                        // A crescent takes the half of that ellipse on the lit side; a gibbous moon
                        // takes the half on the other, which is what closes the greater part of the disc.
                        val crescent = terminator > 0f
                        val sweep = when {
                            waxing && crescent -> -180f
                            waxing -> 180f
                            crescent -> 180f
                            else -> -180f
                        }
                        path.arcTo(terminatorBounds, 90f, sweep, false)
                        path.close()
                    }
                    drawPath(
                        path,
                        brush = Brush.linearGradient(
                            colors = listOf(litHighlight, colours.moonLit, colours.moonShadow),
                            start = Offset(centre.x + if (waxing) radius else -radius, centre.y),
                            end = Offset(centre.x - if (waxing) radius else -radius, centre.y),
                        ),
                    )
                }
            }

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        colours.hairline.copy(alpha = 0.35f),
                        colours.moonlight.copy(alpha = 0.55f),
                        colours.hairline.copy(alpha = 0.85f),
                        colours.moonlight.copy(alpha = 0.45f),
                    ),
                    center = centre,
                ),
                radius = radius,
                center = centre,
                style = Stroke(width = rimWidth),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = phaseName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Above this illuminated fraction the disc is drawn full: the terminator has left the disc. */
private const val FULL = 0.995

/** Below this the disc is drawn new: nothing is lit, and a sliver would be a lie. */
private const val NEW = 0.005

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 320)
@Composable
private fun PhaseDialWaxingPreview() {
    LumintraTheme(darkTheme = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseDial(fraction = 0.16, phaseName = "Waxing crescent")
            Spacer(modifier = Modifier.height(12.dp))
            PhaseDial(fraction = 0.5, phaseName = "First quarter")
            Spacer(modifier = Modifier.height(12.dp))
            PhaseDial(fraction = 1.0, phaseName = "Full")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 320)
@Composable
private fun PhaseDialWaningPreview() {
    LumintraTheme(darkTheme = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseDial(fraction = -0.78, phaseName = "Waning gibbous")
            Spacer(modifier = Modifier.height(12.dp))
            PhaseDial(fraction = -0.5, phaseName = "Last quarter")
            Spacer(modifier = Modifier.height(12.dp))
            PhaseDial(fraction = -0.04, phaseName = "Waning crescent")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 240)
@Composable
private fun PhaseDialRedModePreview() {
    LumintraTheme(darkTheme = true, redMode = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseDial(fraction = 0.42, phaseName = "Waxing crescent")
        }
    }
}

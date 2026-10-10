package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme

/**
 * The slot's own mark: the horizon the user plans against, the dome of sky above it, a single light
 * rising inside the dome and one star outside it.
 *
 * It is drawn from geometry in this file and is never a library glyph — the vendored UI set is for
 * general-purpose iconography (a back arrow, a bin), and a glyph from it may not represent the brand.
 * The four shapes are the same four the launcher icon and the adaptive layers are built from, so the
 * in-app mark and the installed icon are one identity rather than two drawings of the same idea:
 *
 *  * a **horizon** rule across the lower third — the line the app's whole job is about;
 *  * a **dome** standing on that horizon — the arch of sky the planner reads;
 *  * a **light** inside the dome — the one body the app plans around;
 *  * a **star** outside the dome — the reason for any of it.
 *
 * The structure is carried by the composition, not by hue: the shape reads as one mark at 24 dp and
 * stays legible in red night-vision mode, where every role resolves to a red on black.
 *
 * @param tint the ink of the horizon, the dome and the star. Defaults to the scheme's primary, so a
 *   caller that passes nothing gets the identity's "first light" ink; the light itself is always
 *   [com.gexetr.lumintralightplanner.ui.theme.LumintraColors.markBody], because that is the role the
 *   identity reserved for it.
 */
@Composable
fun LumintraMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val light = LumintraTheme.colors.markBody
    Canvas(modifier = modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp)) {
        val shortest = size.minDimension
        if (shortest <= 0f) return@Canvas

        val centreX = size.width / 2f
        val centreY = size.height / 2f
        // Kept inside the canvas at every size, so the mark is never clipped by a tight parent.
        val radius = shortest / 2f * 0.94f
        val rule = (shortest * 0.075f).coerceAtLeast(1.2f)
        val horizonY = centreY + radius * 0.44f

        // The horizon: drawn as a rule rather than a gradient, the way an almanac rules a page.
        drawLine(
            color = tint,
            start = Offset(centreX - radius * 0.88f, horizonY),
            end = Offset(centreX + radius * 0.88f, horizonY),
            strokeWidth = rule,
            cap = StrokeCap.Round,
        )

        // The dome: an arch standing on the horizon, open at the top.
        val domeRadius = radius * 0.62f
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centreX - domeRadius, horizonY - domeRadius),
            size = Size(domeRadius * 2f, domeRadius * 2f),
            style = Stroke(width = rule, cap = StrokeCap.Round),
        )

        // The light: a teardrop rising to just under the dome's apex, its base on the horizon.
        val topY = horizonY - domeRadius * 0.86f
        val bottomY = horizonY - rule * 0.9f
        val span = bottomY - topY
        val halfWidth = radius * 0.19f
        val flame = Path().apply {
            moveTo(centreX, topY)
            cubicTo(
                centreX + halfWidth * 0.92f, topY + span * 0.42f,
                centreX + halfWidth, topY + span * 0.70f,
                centreX + halfWidth, bottomY - span * 0.16f,
            )
            cubicTo(
                centreX + halfWidth, bottomY,
                centreX + halfWidth * 0.55f, bottomY,
                centreX, bottomY,
            )
            cubicTo(
                centreX - halfWidth * 0.55f, bottomY,
                centreX - halfWidth, bottomY,
                centreX - halfWidth, bottomY - span * 0.16f,
            )
            cubicTo(
                centreX - halfWidth, topY + span * 0.70f,
                centreX - halfWidth * 0.92f, topY + span * 0.42f,
                centreX, topY,
            )
            close()
        }
        drawPath(flame, light)

        // The star: outside the dome, where the app's targets live.
        drawCircle(
            color = tint,
            radius = (shortest * 0.055f).coerceAtLeast(0.7f),
            center = Offset(centreX + radius * 0.66f, centreY - radius * 0.40f),
        )
    }
}

/**
 * The mark beside the app's name, for a header or an empty state.
 *
 * The name is read from `R.string.app_name` rather than written here, so the wordmark is a resource
 * like every other user-visible string and the two declared languages keep their own rendering of
 * it.
 */
@Composable
fun LumintraWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LumintraMark(modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 240)
@Composable
private fun LumintraMarkPreview() {
    LumintraTheme(darkTheme = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LumintraMark(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            LumintraMark(modifier = Modifier.size(72.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 260)
@Composable
private fun LumintraWordmarkPreview() {
    LumintraTheme(darkTheme = true) {
        LumintraWordmark()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 260)
@Composable
private fun LumintraWordmarkRedModePreview() {
    LumintraTheme(darkTheme = true, redMode = true) {
        LumintraWordmark()
    }
}

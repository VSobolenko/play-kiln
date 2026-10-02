package com.gexetr.lawnroundlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The round log's own glyph set.
 *
 * Why these are drawn rather than imported: `.cursor/rules/visual-identity.mdc` bans *"a stock icon
 * as the app's mark or as UI iconography"*, and lists *"a glyph set used as iconography"* as part of
 * what a slot owns. A `material-icons-*` dependency would make the app legible as every other
 * Compose app, which is the failure the rule names — a correct app that looks like an unstyled
 * Material demo is a failed recipe even when every gate is green.
 *
 * The whole family shares one stroke weight and one cap style, derived from [GlyphStrokeRatio], so
 * the set reads as one hand rather than fifteen separate drawings. That single constant is
 * deliberate: a glyph set whose weight drifts per glyph is the most visible way an iconography
 * stops looking drawn and starts looking assembled.
 *
 * Colour never lives here. Every glyph takes [tint], defaulting to `LocalContentColor.current`, so
 * the set inherits the slot's own theme and cannot drift from the palette in `ui/theme/Color.kt`.
 *
 * Accessibility is the call site's decision, not the glyph's: a glyph that is the only content of a
 * control needs an accessible name, and one that merely repeats an adjacent label should stay
 * decorative. So none of these carry a `contentDescription`; the noun is supplied where the noun is
 * known.
 */

/** The box every glyph draws inside. 24 dp is the icon convention the rest of the UI is laid out to. */
private val GlyphSize = 24.dp

/**
 * Stroke weight as a fraction of the box, so a glyph is resolution- and size-independent: the same
 * definition renders correctly at 24 dp in a list row and scaled up in the mark.
 */
private const val GlyphStrokeRatio = 0.105f

/**
 * The one drawing surface every glyph is composed on.
 *
 * Keeping the `Canvas`, the size and the stroke derivation in one place is what makes the family
 * consistent by construction rather than by discipline: a glyph cannot pick its own weight or its
 * own box, because it is handed neither.
 */
@Composable
private fun GlyphBox(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    onDraw: DrawScope.(stroke: Float, colour: Color) -> Unit,
) {
    Canvas(modifier = modifier.size(GlyphSize)) {
        onDraw(size.minDimension * GlyphStrokeRatio, tint)
    }
}

/**
 * One straight stroke, with every coordinate expressed as a fraction of the box.
 *
 * Fractions rather than pixels for the same reason the weight is a ratio: the glyph is defined once
 * and stays correct at any rendered size.
 */
private fun DrawScope.seg(
    colour: Color,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    stroke: Float,
) {
    drawLine(
        color = colour,
        start = Offset(size.width * x1, size.height * y1),
        end = Offset(size.width * x2, size.height * y2),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** A filled dot. */
private fun DrawScope.dot(colour: Color, cx: Float, cy: Float, radius: Float) {
    drawCircle(
        color = colour,
        radius = radius * size.minDimension,
        center = Offset(size.width * cx, size.height * cy),
    )
}

/** An outlined circle. */
private fun DrawScope.ring(colour: Color, cx: Float, cy: Float, radius: Float, stroke: Float) {
    drawCircle(
        color = colour,
        radius = radius * size.minDimension,
        center = Offset(size.width * cx, size.height * cy),
        style = Stroke(width = stroke),
    )
}

/** Back. An arrow rather than a bare chevron: it is a direction, not a collapse. */
@Composable
fun GlyphBack(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.82f, 0.50f, 0.20f, 0.50f, stroke)
        seg(colour, 0.44f, 0.26f, 0.20f, 0.50f, stroke)
        seg(colour, 0.20f, 0.50f, 0.44f, 0.74f, stroke)
    }

/** Add. */
@Composable
fun GlyphAdd(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.50f, 0.20f, 0.50f, 0.80f, stroke)
        seg(colour, 0.20f, 0.50f, 0.80f, 0.50f, stroke)
    }

/** Delete. A bin with a lid and a handle, so it is a bin and not a waste-paper square. */
@Composable
fun GlyphDelete(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.20f, 0.30f, 0.80f, 0.30f, stroke)
        seg(colour, 0.40f, 0.30f, 0.40f, 0.18f, stroke)
        seg(colour, 0.40f, 0.18f, 0.60f, 0.18f, stroke)
        seg(colour, 0.60f, 0.18f, 0.60f, 0.30f, stroke)
        seg(colour, 0.30f, 0.30f, 0.35f, 0.82f, stroke)
        seg(colour, 0.35f, 0.82f, 0.65f, 0.82f, stroke)
        seg(colour, 0.65f, 0.82f, 0.70f, 0.30f, stroke)
    }

/** Edit. A pencil with a visible nib, drawn at an angle so it does not read as a slash. */
@Composable
fun GlyphEdit(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.30f, 0.70f, 0.68f, 0.32f, stroke)
        seg(colour, 0.60f, 0.24f, 0.76f, 0.40f, stroke)
        seg(colour, 0.20f, 0.80f, 0.30f, 0.70f, stroke)
        seg(colour, 0.20f, 0.80f, 0.36f, 0.76f, stroke)
    }

/** Lock. A squared shackle, deliberately not the rounded library shape. */
@Composable
fun GlyphLock(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.26f, 0.46f, 0.26f, 0.82f, stroke)
        seg(colour, 0.26f, 0.82f, 0.74f, 0.82f, stroke)
        seg(colour, 0.74f, 0.82f, 0.74f, 0.46f, stroke)
        seg(colour, 0.74f, 0.46f, 0.26f, 0.46f, stroke)
        seg(colour, 0.38f, 0.46f, 0.38f, 0.32f, stroke)
        seg(colour, 0.38f, 0.32f, 0.62f, 0.32f, stroke)
        seg(colour, 0.62f, 0.32f, 0.62f, 0.46f, stroke)
    }

/** Export. An arrow leaving a tray — the action, not the destination app it happens to open. */
@Composable
fun GlyphExport(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.50f, 0.70f, 0.50f, 0.20f, stroke)
        seg(colour, 0.36f, 0.34f, 0.50f, 0.20f, stroke)
        seg(colour, 0.50f, 0.20f, 0.64f, 0.34f, stroke)
        seg(colour, 0.22f, 0.60f, 0.22f, 0.82f, stroke)
        seg(colour, 0.22f, 0.82f, 0.78f, 0.82f, stroke)
        seg(colour, 0.78f, 0.82f, 0.78f, 0.60f, stroke)
    }

/** A customer. Head and shoulders, which is what the operator is looking for in a row. */
@Composable
fun GlyphCustomer(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        ring(colour, 0.50f, 0.32f, 0.15f, stroke)
        seg(colour, 0.24f, 0.82f, 0.32f, 0.62f, stroke)
        seg(colour, 0.32f, 0.62f, 0.68f, 0.62f, stroke)
        seg(colour, 0.68f, 0.62f, 0.76f, 0.82f, stroke)
    }

/** Move up the round. */
@Composable
fun GlyphMoveUp(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.26f, 0.60f, 0.50f, 0.36f, stroke)
        seg(colour, 0.50f, 0.36f, 0.74f, 0.60f, stroke)
        seg(colour, 0.50f, 0.36f, 0.50f, 0.80f, stroke)
    }

/** Move down the round. */
@Composable
fun GlyphMoveDown(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.26f, 0.40f, 0.50f, 0.64f, stroke)
        seg(colour, 0.50f, 0.64f, 0.74f, 0.40f, stroke)
        seg(colour, 0.50f, 0.20f, 0.50f, 0.64f, stroke)
    }

/** Mowed. */
@Composable
fun GlyphMowed(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.22f, 0.54f, 0.42f, 0.74f, stroke)
        seg(colour, 0.42f, 0.74f, 0.80f, 0.28f, stroke)
    }

/** Skipped. */
@Composable
fun GlyphSkipped(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.26f, 0.26f, 0.74f, 0.74f, stroke)
        seg(colour, 0.74f, 0.26f, 0.26f, 0.74f, stroke)
    }

/**
 * Rained off. A cloud dome with rain under it, which is the weather rather than a generic warning —
 * the operator reads the reason, not an alarm.
 */
@Composable
fun GlyphRain(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        drawArc(
            color = colour,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.18f),
            size = Size(size.width * 0.56f, size.height * 0.40f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        seg(colour, 0.34f, 0.52f, 0.28f, 0.74f, stroke)
        seg(colour, 0.52f, 0.52f, 0.46f, 0.74f, stroke)
        seg(colour, 0.70f, 0.52f, 0.64f, 0.74f, stroke)
    }

/** A warning, for the cases that are neither weather nor a plain skip. */
@Composable
fun GlyphWarning(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.50f, 0.16f, 0.86f, 0.80f, stroke)
        seg(colour, 0.86f, 0.80f, 0.14f, 0.80f, stroke)
        seg(colour, 0.14f, 0.80f, 0.50f, 0.16f, stroke)
        seg(colour, 0.50f, 0.40f, 0.50f, 0.58f, stroke)
        dot(colour, 0.50f, 0.69f, 0.045f)
    }

/** A date, for the round's own day. */
@Composable
fun GlyphDate(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.20f, 0.28f, 0.80f, 0.28f, stroke)
        seg(colour, 0.80f, 0.28f, 0.80f, 0.82f, stroke)
        seg(colour, 0.80f, 0.82f, 0.20f, 0.82f, stroke)
        seg(colour, 0.20f, 0.82f, 0.20f, 0.28f, stroke)
        seg(colour, 0.20f, 0.44f, 0.80f, 0.44f, stroke)
        seg(colour, 0.36f, 0.16f, 0.36f, 0.32f, stroke)
        seg(colour, 0.64f, 0.16f, 0.64f, 0.32f, stroke)
        dot(colour, 0.36f, 0.62f, 0.055f)
    }

/**
 * Settings. Sliders rather than a gear: the gear is the shape every app in the store already has,
 * and this screen's job is to change a handful of real values, which is what a slider says.
 */
@Composable
fun GlyphSettings(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        seg(colour, 0.18f, 0.30f, 0.82f, 0.30f, stroke)
        seg(colour, 0.18f, 0.50f, 0.82f, 0.50f, stroke)
        seg(colour, 0.18f, 0.70f, 0.82f, 0.70f, stroke)
        dot(colour, 0.40f, 0.30f, 0.085f)
        dot(colour, 0.62f, 0.50f, 0.085f)
        dot(colour, 0.30f, 0.70f, 0.085f)
    }

/** Information. */
@Composable
fun GlyphInfo(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    GlyphBox(modifier, tint) { stroke, colour ->
        ring(colour, 0.50f, 0.50f, 0.33f, stroke)
        dot(colour, 0.50f, 0.33f, 0.050f)
        seg(colour, 0.50f, 0.45f, 0.50f, 0.68f, stroke)
    }

/**
 * The whole set on one surface.
 *
 * A glyph set is judged by whether it reads as one family, and that is not visible one glyph at a
 * time — so the preview shows the family, at the size the UI actually draws it.
 */
@Preview(showBackground = true)
@Composable
private fun GlyphSetPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphBack()
                    GlyphAdd()
                    GlyphDelete()
                    GlyphEdit()
                    GlyphLock()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphExport()
                    GlyphCustomer()
                    GlyphMoveUp()
                    GlyphMoveDown()
                    GlyphMowed()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphSkipped()
                    GlyphRain()
                    GlyphWarning()
                    GlyphDate()
                    GlyphSettings()
                    GlyphInfo()
                }
            }
        }
    }
}

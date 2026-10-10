package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.LumintraType

/** The one radius a badge or a band rule is rounded by. */
private val PillShape = RoundedCornerShape(percent = 50)

/**
 * A source badge: where a number came from and how old it is, e.g. `MET Norway · forecast · 2 h old`.
 *
 * The text is the caller's own localised string, because the source's name and the age's units belong
 * to the data layer, not here.
 *
 * @param stale switches the badge to the identity's caution ink and turns the status dot into a
 *   hollow ring. Two signals rather than one, because colour alone is not an accessible state — the
 *   ring survives a monochrome reading, red night-vision mode and a colour-blind viewer.
 */
@Composable
fun SourceBadge(
    text: String,
    stale: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ink = if (stale) {
        LumintraTheme.colors.caution
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val container = if (stale) {
        LumintraTheme.colors.caution.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(container)
            .border(1.dp, ink.copy(alpha = 0.36f), PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            val strokeWidth = 1.5.dp.toPx()
            if (stale) {
                drawCircle(
                    color = ink,
                    radius = size.minDimension / 2f - strokeWidth / 2f,
                    style = Stroke(width = strokeWidth),
                )
            } else {
                drawCircle(color = ink, radius = size.minDimension / 2f)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ink,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
    }
}

/**
 * A horizontal compass strip: where the target is on the horizon, read against the cardinal points.
 *
 * The strip runs north to north left to right and is **not mirrored in a right-to-left layout**: a
 * compass is a physical scale, not prose, and a mirrored azimuth would put west on the right of the
 * screen while the sky puts it on the left.
 *
 * The cardinal names are resources rather than literals, so the strip reads `N NE E …` in English and
 * `С СВ В …` in Russian — the four intercardinals are spelled differently in the two languages, and
 * this is exactly the kind of short label a hardcoded literal would have shipped in English.
 *
 * The target is marked twice: a filled marker on the baseline, which is the readable position, and a
 * rule dropping below the strip, which is what the eye follows from the marker to the value in the
 * panel beside it.
 */
@Composable
fun AzimuthRuler(
    azimuthDeg: Double,
    modifier: Modifier = Modifier,
) {
    val colours = LumintraTheme.colors
    val hairline = colours.hairline
    val targetInk = colours.firstLight
    val labelInk = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = remember {
        TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
        )
    }
    val labels = listOf(
        stringResource(R.string.component_compass_n),
        stringResource(R.string.component_compass_ne),
        stringResource(R.string.component_compass_e),
        stringResource(R.string.component_compass_se),
        stringResource(R.string.component_compass_s),
        stringResource(R.string.component_compass_sw),
        stringResource(R.string.component_compass_w),
        stringResource(R.string.component_compass_nw),
        stringResource(R.string.component_compass_n),
    )
    val labelLayouts = remember(labels, labelStyle) {
        labels.map { measurer.measure(AnnotatedString(it), style = labelStyle) }
    }
    val target = azimuthDeg.coerceIn(0.0, 360.0).toFloat() / 360f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val labelTop = 2.dp.toPx()
        val tickTop = 18.dp.toPx()
        val tickLong = 1.4.dp.toPx()
        val baselineY = 30.dp.toPx()

        // A tick every 15 degrees, longer every 45: enough to read a position, not a protractor.
        for (step in 0..24) {
            val x = width * (step * 15f) / 360f
            val major = step % 3 == 0
            drawLine(
                color = hairline,
                start = Offset(x, if (major) tickTop else tickTop + 4.dp.toPx()),
                end = Offset(x, baselineY),
                strokeWidth = if (major) 1.2.dp.toPx() else 1.dp.toPx(),
            )
        }
        drawLine(
            color = hairline,
            start = Offset(0f, baselineY),
            end = Offset(width, baselineY),
            strokeWidth = 1.dp.toPx(),
        )

        labelLayouts.forEachIndexed { index, layout ->
            val x = width * (index * 45f) / 360f
            val maxLeft = (width - layout.size.width).coerceAtLeast(0f)
            val left = (x - layout.size.width / 2f).coerceIn(0f, maxLeft)
            drawText(
                textLayoutResult = layout,
                color = labelInk,
                topLeft = Offset(left, labelTop),
            )
        }

        val markX = width * target
        drawLine(
            color = targetInk,
            start = Offset(markX, baselineY),
            end = Offset(markX, height - 6.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = targetInk,
            start = Offset(markX - 5.dp.toPx(), height - 6.dp.toPx()),
            end = Offset(markX + 5.dp.toPx(), height - 6.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = targetInk, radius = 3.4.dp.toPx(), center = Offset(markX, baselineY))
    }
}

/**
 * One row of the key-times table: the band's name, its start and its end.
 *
 * @param twilight colours the row's band rule with the identity's blue hour rather than its first
 *   light, so the three Sun-centre crossings read as one family and golden hour / blue hour as
 *   another. The distinction is a *drawing* of `KeyTimeBand.isTwilight`, never a re-derivation of it.
 * @param startLabel / @param endLabel a band that does not occur on this night (a polar night or day)
 *   passes `null`, and the row then draws the identity's "no value" mark — a short hairline — instead
 *   of a dash or a fabricated time. The cells keep a minimum width so a table of rows stays aligned,
 *   and that minimum is a `widthIn`, never a fixed width: a longer time widens its own cell.
 */
@Composable
fun KeyTimeRow(
    bandLabel: String,
    startLabel: String?,
    endLabel: String?,
    twilight: Boolean,
    modifier: Modifier = Modifier,
) {
    val bandInk = if (twilight) LumintraTheme.colors.blueHour else LumintraTheme.colors.firstLight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(bandInk),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = bandLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
        Spacer(modifier = Modifier.width(8.dp))
        TimeCell(label = startLabel, align = TextAlign.End)
        Icon(
            painter = painterResource(R.drawable.ic_ui_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(14.dp),
        )
        TimeCell(label = endLabel, align = TextAlign.Start)
    }
}

/** One time in a [KeyTimeRow], or the absent mark when the band does not occur. */
@Composable
private fun TimeCell(label: String?, align: TextAlign) {
    if (label == null) {
        Box(
            modifier = Modifier
                .widthIn(min = 56.dp)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AbsentHairline()
        }
    } else {
        Text(
            text = label,
            style = LumintraType.clockSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = align,
            modifier = Modifier.widthIn(min = 56.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The identity's mark for a value that does not exist: a ruled hairline, never a dash. */
@Composable
private fun AbsentHairline() {
    val ink = LumintraTheme.colors.hairline
    Canvas(modifier = Modifier.size(width = 14.dp, height = 2.dp)) {
        drawRoundRect(
            color = ink,
            size = this.size,
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun AzimuthRulerPreview() {
    LumintraTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(12.dp)) {
            AzimuthRuler(azimuthDeg = 214.0)
            Spacer(modifier = Modifier.height(8.dp))
            AzimuthRuler(azimuthDeg = 18.0)
            Spacer(modifier = Modifier.height(8.dp))
            AzimuthRuler(azimuthDeg = 359.0)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun KeyTimeTablePreview() {
    LumintraTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(12.dp)) {
            KeyTimeRow(
                bandLabel = "Golden hour",
                startLabel = "19:41",
                endLabel = "20:26",
                twilight = false,
            )
            KeyTimeRow(
                bandLabel = "Blue hour",
                startLabel = "20:26",
                endLabel = "21:02",
                twilight = false,
            )
            KeyTimeRow(
                bandLabel = "Astronomical",
                startLabel = "22:41",
                endLabel = "03:12",
                twilight = true,
            )
            KeyTimeRow(
                bandLabel = "Nautical",
                startLabel = null,
                endLabel = null,
                twilight = true,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SourceBadge(text = "MET Norway · forecast · 2 h old", stale = false)
        Spacer(modifier = Modifier.height(6.dp))
        SourceBadge(text = "MET Norway · forecast · 31 h old", stale = true)
    }
}

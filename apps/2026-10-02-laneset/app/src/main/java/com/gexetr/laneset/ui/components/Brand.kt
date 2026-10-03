package com.gexetr.laneset.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.laneset.R
import com.gexetr.laneset.ui.theme.DeckSlabShape
import com.gexetr.laneset.ui.theme.LaneRopeShape
import com.gexetr.laneset.ui.theme.laneSetColors
import com.gexetr.laneset.ui.theme.laneSetTypography
import kotlin.math.cos
import kotlin.math.sin

/**
 * Laneset's component vocabulary.
 *
 * These are the things this app says over and over, written once each: a target time on its own
 * panel, a focus the swimmer picks between, a labelled number, a verdict on whether a set can be
 * held, and the mark itself. They are slot code rather than wrappers around `Card`, and every one of
 * them encodes a decision the app makes repeatedly — which is the test a component has to pass to
 * live here. A component that wraps `Card` adds nothing but a second name for the same thing.
 *
 * Three rules hold across the file, and each one is why a reader will find the code shaped the way
 * it is:
 *
 * - **No literals for colour.** Every tint comes from `MaterialTheme.colorScheme`, from
 *   `laneSetColors` (the slot's domain roles) or from `LocalContentColor`, so the light scheme, the
 *   dark scheme and the store pack's own palette are one decision rather than three.
 * - **No literal for text.** Every user-facing string is `stringResource(…)`. The strings this file
 *   needs and does not own are listed in the handoff rather than added to a file another worker has
 *   open.
 * - **The numeric styles, not the display ones, for anything with a digit in it.** `laneSetTypography`
 *   carries tabular figures, so a recomputed target time does not change the width of the line it
 *   sits on and a column of splits stays a column.
 */

// ---------------------------------------------------------------------------------------------
// The mark
// ---------------------------------------------------------------------------------------------

/**
 * The mark's geometry, in fractions of *half* the canvas, from `scripts/lib/icon_design.py`'s
 * `mark-clock` primitive set.
 *
 * These four numbers are the same four the launcher vectors and the store icon are built from, and
 * they are restated here rather than shared because Kotlin cannot import a Python module: the ring's
 * outer edge lands on the circle keyline (0.574 of the half-canvas, the same 31dp the adaptive-icon
 * grid declares), the minute hand runs to 60 degrees and the hour hand back to 200, and both stop a
 * stroke and a half short of the dial so the face never fills in. `scripts/test-icon-design.py`
 * measures the two renderers against each other; this file is the third reader of the same numbers.
 */
private const val MARK_RING_OUTER = 0.5741f
private const val MARK_STROKE = 0.09375f
private const val MARK_MINUTE_LENGTH = 0.3397f
private const val MARK_HOUR_LENGTH = 0.2310f
private const val MARK_MINUTE_ANGLE_DEG = 60.0
private const val MARK_HOUR_ANGLE_DEG = 200.0

/**
 * The brand mark: a pool-deck pace clock.
 *
 * Drawn, not borrowed — this is the slot's own glyph, the same geometry as the launcher icon and the
 * store icon, which is why it is a `Canvas` here and why a stock library icon is not an acceptable
 * substitute. The ring and the hour hand take [tint] and the minute hand takes [accent], so the mark
 * reads on a light whiteboard and on the dark hero from the same code: on a hero panel the local
 * content colour is the paper ink and the accent is the pace gold, and on a white surface both
 * resolve to their readable forms.
 *
 * [contentDescription] is `null` by default because the mark is decorative wherever this app uses
 * it — it always sits inside a panel that already says what the panel is. A caller that puts it
 * somewhere it carries meaning passes a label.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    accent: Color = laneSetColors.pace,
) {
    val label = contentDescription
    Canvas(modifier = markSemantics(label)) {
        val half = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = MARK_STROKE * half
        // The ring is stroked on its centreline so its *outer* edge — what a reader perceives as the
        // mark's size — lands on the declared keyline rather than a half-stroke inside it.
        val ringRadius = (MARK_RING_OUTER - MARK_STROKE / 2f) * half
        drawCircle(
            color = tint,
            radius = ringRadius,
            center = centre,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = accent,
            start = centre,
            end = handEnd(centre, MARK_MINUTE_LENGTH * half, MARK_MINUTE_ANGLE_DEG),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = centre,
            end = handEnd(centre, MARK_HOUR_LENGTH * half, MARK_HOUR_ANGLE_DEG),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** A point [length] from [centre] at [angleDeg], measured counter-clockwise from three o'clock. */
private fun handEnd(centre: Offset, length: Float, angleDeg: Double): Offset {
    val radians = Math.toRadians(angleDeg)
    return Offset(
        x = centre.x + length * cos(radians).toFloat(),
        y = centre.y - length * sin(radians).toFloat(),
    )
}

/** The mark's semantics, applied only when it actually carries meaning. */
private fun markSemantics(label: String?): Modifier =
    if (label == null) Modifier else Modifier.semantics { contentDescription = label }

// ---------------------------------------------------------------------------------------------
// The target time
// ---------------------------------------------------------------------------------------------

/**
 * The interval hero: the distance and stroke in words, and the target time as the largest datum on
 * the screen.
 *
 * This is the panel the whole app exists to produce, so it is the one place in the app allowed a
 * gradient field and the one place the pace gold is used at full chroma. The three numbers are set in
 * `laneSetTypography`, which carries tabular figures: `1:45` and `1:00` are the same width, so the
 * panel does not twitch as the swimmer changes their minutes.
 *
 * The labels (`[brand_target_label]`, `[brand_total_label]`, `[brand_pace_label]`) are the panel's own
 * vocabulary and are read here; the values, the distance and the stroke name are the caller's data.
 */
@Composable
fun PaceHeroPanel(
    intervalHeadline: String,
    strokeLabel: String,
    targetTime: String,
    totalValue: String,
    paceValue: String,
    modifier: Modifier = Modifier,
) {
    val laneSet = laneSetColors
    val numeric = MaterialTheme.laneSetTypography
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(laneSet.heroStart, laneSet.heroEnd)),
                shape = DeckSlabShape,
            )
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = laneSet.onHeroMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.brand_target_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = laneSet.onHeroMuted,
                )
            }
            Text(
                text = intervalHeadline,
                style = numeric.intervalHeadline,
                color = laneSet.onHero,
            )
            Text(
                text = targetTime,
                style = numeric.intervalTarget,
                color = laneSet.pace,
            )
            Text(
                text = strokeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = laneSet.onHeroMuted,
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                HeroMetric(
                    label = stringResource(R.string.brand_total_label),
                    value = totalValue,
                    valueColor = laneSet.onHero,
                    labelColor = laneSet.onHeroMuted,
                )
                HeroMetric(
                    label = stringResource(R.string.brand_pace_label),
                    value = paceValue,
                    valueColor = laneSet.onHero,
                    labelColor = laneSet.onHeroMuted,
                )
            }
        }
    }
}

/** One labelled number inside the hero. Private: the hero's own layout, not a general tile. */
@Composable
private fun HeroMetric(label: String, value: String, valueColor: Color, labelColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
        )
        Text(
            text = value,
            style = MaterialTheme.laneSetTypography.paceSplit,
            color = valueColor,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The focus the swimmer picks
// ---------------------------------------------------------------------------------------------

/**
 * One choice in a [FocusSelectorStrip].
 *
 * `id` is the key the caller stores and compares; `label` is what the swimmer reads, already resolved
 * from a resource by the screen that owns the vocabulary. Keeping the label out of this file is what
 * lets the strip be reused by a screen whose focus names are its own.
 */
@Immutable
data class FocusOption(val id: String, val label: String)

/**
 * The day's focus, as a strip the swimmer picks between.
 *
 * A rope-and-pill strip rather than a row of tabs: each choice is a lane-rope cap at any height, so
 * it stays a pill in a longer locale rather than becoming a rounded rectangle. The selected choice is
 * a filled lane-rope blue with its own ink; the others are a hairline on the surface. It scrolls
 * horizontally because a pool's own focus vocabulary is longer than a phone is wide, and a wrapped row
 * of unequal widths is how a selector stops reading as one control.
 *
 * Every choice is a `Role.RadioButton` at a 48dp target: it is a single choice out of a set, not a
 * toggle, and TalkBack should say so.
 */
@Composable
fun FocusSelectorStrip(
    options: List<FocusOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val laneSet = laneSetColors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.brand_focus_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val selected = option.id == selectedId
                Surface(
                    shape = LaneRopeShape,
                    color = if (selected) laneSet.lane else Color.Transparent,
                    contentColor = if (selected) laneSet.onLane else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.id) },
                        ),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// A labelled number
// ---------------------------------------------------------------------------------------------

/**
 * One labelled number in a grid of them — a section's distance, a stroke's share, a session total.
 *
 * [icon] is optional and is one of the vendored drawables, tinted from the tile's own accent rather
 * than carrying a colour of its own. The value is set in the numeric style, so two tiles side by side
 * keep their digits in the same columns.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: Int? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.laneSetTypography.paceSplit,
                color = accent,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Whether the set can be held
// ---------------------------------------------------------------------------------------------

/**
 * What the app can say about a computed interval.
 *
 * Three states, and no fourth: the target is inside the pace the trials imply, it is inside it with
 * little room, or it is outside what the swimmer can hold. The screen maps its own domain value onto
 * this, so this file never depends on the domain layer's types.
 */
enum class SetVerdict {
    /** The pace the trials imply covers this interval. */
    Holdable,

    /** It covers it, but there is little room — the swimmer will work for it. */
    Tight,

    /** The interval cannot be swum at the pace the trials imply. */
    OverPace,
}

/**
 * The verdict on a set, as a pill: a word, an icon and a colour, all three from the theme.
 *
 * Colour alone is never the message — the word carries it and the icon repeats it, because a red
 * pill and an amber pill are the same pill to a swimmer who cannot separate the two hues. The
 * container and its ink come from `laneSetColors`, so "over pace" is the same colour in the app as it
 * is in the pack.
 */
@Composable
fun StatusPill(verdict: SetVerdict, modifier: Modifier = Modifier) {
    val laneSet = laneSetColors
    val container = when (verdict) {
        SetVerdict.Holdable -> laneSet.rest
        SetVerdict.Tight -> laneSet.paceContainer
        SetVerdict.OverPace -> laneSet.alarm
    }
    val content = when (verdict) {
        SetVerdict.Holdable -> laneSet.onRest
        SetVerdict.Tight -> laneSet.onPaceContainer
        SetVerdict.OverPace -> laneSet.onAlarm
    }
    val label = stringResource(
        when (verdict) {
            SetVerdict.Holdable -> R.string.brand_status_holdable
            SetVerdict.Tight -> R.string.brand_status_tight
            SetVerdict.OverPace -> R.string.brand_status_over_pace
        },
    )
    val icon = when (verdict) {
        SetVerdict.Holdable -> R.drawable.ic_check
        SetVerdict.Tight -> R.drawable.ic_info
        SetVerdict.OverPace -> R.drawable.ic_close
    }
    Surface(
        shape = LaneRopeShape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

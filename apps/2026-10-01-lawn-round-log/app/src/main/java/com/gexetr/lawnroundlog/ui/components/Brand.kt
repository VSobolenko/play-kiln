package com.gexetr.lawnroundlog.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lawnroundlog.R
import com.gexetr.lawnroundlog.domain.VisitStatus
import com.gexetr.lawnroundlog.ui.theme.LawnRoundLogTheme
import com.gexetr.lawnroundlog.ui.theme.PlotCardShape
import com.gexetr.lawnroundlog.ui.theme.TallyNumerals
import com.gexetr.lawnroundlog.ui.theme.VisitMowedShape
import com.gexetr.lawnroundlog.ui.theme.VisitRainedOffShape
import com.gexetr.lawnroundlog.ui.theme.VisitSkippedShape
import java.text.NumberFormat
import kotlin.math.min

// The slot's visual vocabulary. Everything a screen needs to look like this app rather than like a
// Material demo lives here, and each component encodes a decision the app makes over and over: the
// mark is the mown stripes, the pill states a visit without relying on hue, a stat tile is a label
// over a tallied number, and the section strip is the app's one navigation-looking control.
//
// The rest of the vocabulary — the 24 dp working glyphs the round, the ledger, the customer form and
// the shell are built from — is in `Glyphs.kt` beside this file, and is drawn to the same rules the
// mark is: geometry, not an icon font.
//
// Ground rules these components are held to, all of them measured by `node scripts/verify.mjs`
// (scripts/core/uiux.mjs): no fixed `width`/`height`/`size` on anything that holds text, so a label
// is never squeezed into a box it cannot grow out of; no `maxLines = 1` without an `overflow`, so a
// clamp can always say it truncated; every interactive element carries a name and at least a 48 dp
// target; and every state change animates rather than snapping.

// ---------------------------------------------------------------------------------------------
// The mark
// ---------------------------------------------------------------------------------------------

/** The mown-stripe fan, in canvas fractions of the 108dp icon grid `mark-stripes` is built on. */
private const val MARK_TOP_FRACTION = 0.350f
private const val MARK_BOTTOM_FRACTION = 0.650f

/** The declared `bold` stroke: 24px of the 512px icon canvas. */
private const val MARK_STROKE_FRACTION = 24f / 512f

/** (topX, bottomX, ink) for each stripe: two chalk outer stripes around two cut-accent inner ones. */
private val MARK_STRIPES = listOf(
    Triple(0.385f, 0.315f, 0.55f),
    Triple(0.462f, 0.438f, 1f),
    Triple(0.538f, 0.562f, 1f),
    Triple(0.615f, 0.685f, 0.55f),
)

/**
 * The slot's mark: four mown stripes converging toward a vanishing point above them.
 *
 * Drawn from geometry rather than borrowed from an icon font, so it is the same glyph the launcher
 * icon is built from and it carries no Material, emoji or clipart ancestry. At 48 dp the four
 * strokes are 2.25 dp wide with clear gaps between them, so it still reads as a cut row rather than
 * as a smudge.
 *
 * The caller decides the size; the default is the 48 dp floor so a bare `RoundMark()` is never a
 * zero-sized hole. A caller that passes its own extent wins, because this adds its default inside
 * the incoming constraints.
 */
@Composable
fun RoundMark(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(48.dp)) {
        val unit = min(size.width, size.height)
        val stroke = unit * MARK_STROKE_FRACTION
        for ((topX, bottomX, ink) in MARK_STRIPES) {
            drawLine(
                color = tint.copy(alpha = tint.alpha * ink),
                start = Offset(topX * unit, MARK_TOP_FRACTION * unit),
                end = Offset(bottomX * unit, MARK_BOTTOM_FRACTION * unit),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Visit status
// ---------------------------------------------------------------------------------------------

/**
 * How one visit state looks: a word to read, a silhouette to recognise and a pair of colours.
 *
 * The shape is the point of this holder. The round-log spec requires a status to be stated with a
 * word or a shape in addition to colour alone, so the three shapes are deliberately different from
 * each other and not three tints of one pill - a full pill for a job that is done, a chamfered tag
 * for one that was skipped, a squared one for a morning the weather cancelled.
 */
private data class VisitStatusStyle(
    val labelRes: Int,
    val shape: Shape,
    val container: Color,
    val content: Color,
)

/**
 * The one `when` over [VisitStatus].
 *
 * Exhaustive on purpose and with no `else`: the compiler is what keeps a fourth status from being
 * added to the domain and silently rendered as nothing.
 */
@Composable
private fun visitStatusStyle(status: VisitStatus): VisitStatusStyle {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        VisitStatus.MOWED -> VisitStatusStyle(
            labelRes = R.string.visit_mowed,
            shape = VisitMowedShape,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
        )

        VisitStatus.SKIPPED -> VisitStatusStyle(
            labelRes = R.string.visit_skipped,
            shape = VisitSkippedShape,
            container = scheme.tertiaryContainer,
            content = scheme.onTertiaryContainer,
        )

        VisitStatus.RAINED_OFF -> VisitStatusStyle(
            labelRes = R.string.visit_rained_off,
            shape = VisitRainedOffShape,
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
        )
    }
}

/**
 * One visit state as a word and a shape: the mark, the status in the user's language, and a
 * silhouette that survives being read by someone who cannot tell the palette's hues apart.
 *
 * The label is a resource, so the pill is as readable in the longest supported locale as in the
 * shortest, and the pill grows around it instead of clamping it.
 */
@Composable
fun VisitStatusPill(
    status: VisitStatus,
    modifier: Modifier = Modifier,
) {
    val style = visitStatusStyle(status)
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .animateContentSize(),
        shape = style.shape,
        color = style.container,
        contentColor = style.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoundMark(modifier = Modifier.size(18.dp), tint = style.content)
            Text(
                text = stringResource(style.labelRes),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tiles and balances
// ---------------------------------------------------------------------------------------------

/**
 * A label over a tallied value - one number from the round, set to be read at arm's length.
 *
 * Both strings are the caller's: the label is a resource from the screen that owns the noun, and the
 * value is already formatted for its own locale. The tile animates its own size, so a total that
 * grows from one digit to three moves instead of jumping.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.animateContentSize(),
        shape = PlotCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = TallyNumerals,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * One customer and what he still owes.
 *
 * The amount is tinted rather than coloured flat: an outstanding balance takes the cut accent and a
 * settled one drops back to secondary ink, and the change animates so that recording a payment is
 * visibly a change of state and not merely a new number.
 *
 * The name is flexible and the amount is not - the name is what wraps when it is long, and the
 * amount is what stays whole.
 */
@Composable
fun BalanceLine(
    name: String,
    amountUsd: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val settled = amountUsd <= 0.0
    val amountColor by animateColorAsState(
        targetValue = if (settled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "balanceLineAmount",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = tallyAmount(amountUsd, currencySymbol),
            style = TallyNumerals,
            color = amountColor,
        )
    }
}

/**
 * An amount, grouped and rounded by the device's own locale rather than by a hand-written pattern.
 *
 * The currency symbol is the caller's, because the slot must not invent a currency it was not told
 * about; the digits and the grouping are the platform's, so no separator decision is compiled in.
 */
private fun tallyAmount(amountUsd: Double, currencySymbol: String): String {
    val format = NumberFormat.getNumberInstance()
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return "$currencySymbol ${format.format(amountUsd)}"
}

// ---------------------------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------------------------

/**
 * What the round shows when there is nothing in it yet.
 *
 * The mark is the empty state's iconography rather than a stock illustration, and both strings are
 * the caller's resources: the body is where a screen explains in the user's language what to do
 * first, and it may be as long as that language needs because nothing here is clamped or boxed.
 */
@Composable
fun RoundEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundMark(
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Section strip
// ---------------------------------------------------------------------------------------------

/**
 * The slot's own strip of sections, each one a labelled target with the mark beside it.
 *
 * It is laid out to survive translation rather than to look tidy in English: the strip scrolls
 * horizontally and every label may wrap to two lines with an ellipsis, so the longest supported
 * locale widens the row instead of clipping it. The deliberate alternative - hiding an unselected
 * item's label once it stops fitting - would have to guess how wide the longest language is, and a
 * layout whose style depends on English string lengths is exactly the defect the visual-identity
 * rule names; a scrolling strip depends on no string length at all.
 *
 * Each item is one target of at least 48 dp with its label as the accessible name, and the selected
 * item is marked by fill, by weight and by silhouette rather than by colour alone.
 */
@Composable
fun SectionStrip(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val container by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "sectionStripContainer",
            )
            val content by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "sectionStripContent",
            )
            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = item, onClick = { onSelect(index) }),
                shape = if (selected) {
                    MaterialTheme.shapes.medium
                } else {
                    MaterialTheme.shapes.small
                },
                color = container,
                contentColor = content,
            ) {
                Row(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoundMark(modifier = Modifier.size(18.dp), tint = content)
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

@Preview(showBackground = true, name = "Visit statuses - light")
@Composable
private fun VisitStatusPillPreview() {
    LawnRoundLogTheme {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VisitStatusPill(VisitStatus.MOWED)
            VisitStatusPill(VisitStatus.SKIPPED)
            VisitStatusPill(VisitStatus.RAINED_OFF)
        }
    }
}

@Preview(showBackground = true, name = "Visit statuses - dark")
@Composable
private fun VisitStatusPillDarkPreview() {
    LawnRoundLogTheme(darkTheme = true) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VisitStatusPill(VisitStatus.MOWED)
            VisitStatusPill(VisitStatus.SKIPPED)
            VisitStatusPill(VisitStatus.RAINED_OFF)
        }
    }
}

@Preview(showBackground = true, name = "Stat tile and balance lines")
@Composable
private fun BalancePreview() {
    LawnRoundLogTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(label = "Owed this week", value = "$ 128.00")
            BalanceLine(name = "Ada Reyes", amountUsd = 80.0, currencySymbol = "$")
            BalanceLine(name = "Marek Nowak", amountUsd = 0.0, currencySymbol = "$")
        }
    }
}

@Preview(showBackground = true, name = "Empty round")
@Composable
private fun RoundEmptyStatePreview() {
    LawnRoundLogTheme {
        RoundEmptyState(
            title = "No customers yet",
            body = "Add the first address on the round and it becomes the first row.",
        )
    }
}

@Preview(showBackground = true, name = "Section strip")
@Composable
private fun SectionStripPreview() {
    LawnRoundLogTheme {
        SectionStrip(
            items = listOf("Today", "Customers", "Money", "Settings"),
            selectedIndex = 1,
            onSelect = {},
        )
    }
}

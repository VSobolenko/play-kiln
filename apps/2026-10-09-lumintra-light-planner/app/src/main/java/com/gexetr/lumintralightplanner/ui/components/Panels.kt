package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.LumintraType

/**
 * The panel corner, 14 dp: the identity's own radius for a plate of glass, deliberately not one of
 * Material 3's. It is a constant here rather than a theme role because only the panel uses it — a
 * second reader would be a second owner.
 */
private val PanelShape = RoundedCornerShape(14.dp)

/**
 * The slot's panel: a tonal surface, one hairline outline and a ruled corner. Not a stock `Card`.
 *
 * @param accent marks the panel that carries the answer the screen exists for — it takes the next
 *   tonal step and is washed with a faint band of the identity's "first light", so a screen has one
 *   place the eye lands first instead of a grid of identical cards.
 */
@Composable
fun LumintraPanel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = if (accent) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val outline = if (accent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val wash = if (accent) {
        Brush.horizontalGradient(
            listOf(LumintraTheme.colors.firstLight.copy(alpha = 0.12f), Color.Transparent),
        )
    } else {
        null
    }
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(container)
            .then(if (wash != null) Modifier.background(wash) else Modifier)
            .border(1.dp, outline, PanelShape)
            .padding(16.dp),
        content = content,
    )
}

/**
 * The screen header the slot owns: the title in the almanac's own voice, with a short rule under it
 * and the back control on the left. Not a stock `TopAppBar`.
 *
 * The header is a row of intrinsic height with a 64 dp minimum, never a fixed height: at the largest
 * system font scale a two-line title must be allowed to grow the header rather than be clipped by it.
 */
@Composable
fun LumintraScaffoldHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_arrow_left),
                    // `nav_back` rather than a new `component_back`: the key already exists in both
                    // locales and carries exactly this word, and INTERFACES.md §3 makes reuse the rule.
                    contentDescription = stringResource(R.string.nav_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        actions()
    }
}

/**
 * A hero reading: the value in the tabular clock voice, its unit stepped down, the label under it.
 *
 * @param emphasised gives the value the "first light" ink and adds the short rule under the label,
 *   which is how one number on a screen is marked as the number that matters.
 */
@Composable
fun HeroStat(
    value: String,
    label: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    val valueColour = if (emphasised) {
        LumintraTheme.colors.firstLight
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = if (emphasised) LumintraType.clockLarge else LumintraType.clock,
                color = valueColour,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = LumintraType.clockSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
        if (emphasised) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(LumintraTheme.colors.firstLight),
            )
        }
    }
}

/**
 * One row of the brief's tables: a label and its value, with an optional second line and an optional
 * leading glyph slot.
 *
 * The label column is weighted, so a long label wraps inside the space the value does not need rather
 * than pushing the value off the row, and neither column declares a fixed extent — the row grows with
 * the font scale instead of clipping.
 */
@Composable
fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(min = 48.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
            )
            if (secondary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = LumintraType.clockSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(min = 48.dp)
                .weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            textAlign = TextAlign.End,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun LumintraHeaderPreview() {
    LumintraTheme(darkTheme = true) {
        Column {
            LumintraScaffoldHeader(title = "Tonight", onBack = {})
            LumintraPanel(modifier = Modifier.padding(horizontal = 12.dp), accent = true) {
                HeroStat(value = "22:41", label = "Astronomical dark", emphasised = true)
                Spacer(modifier = Modifier.height(12.dp))
                ValueRow(label = "Dark window", value = "6 h 12 min", secondary = "Moon sets 23:10")
                ValueRow(label = "Cloud cover", value = "18%")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F1, widthDp = 360)
@Composable
private fun LumintraHeaderLightPreview() {
    LumintraTheme(darkTheme = false) {
        Column {
            LumintraScaffoldHeader(title = "Tonight", onBack = {})
            LumintraPanel(modifier = Modifier.padding(horizontal = 12.dp)) {
                HeroStat(value = "18%", label = "Cloud cover", unit = "of sky")
            }
        }
    }
}

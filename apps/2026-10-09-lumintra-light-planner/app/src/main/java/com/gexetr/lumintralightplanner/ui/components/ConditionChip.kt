package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme

/**
 * The severity a [ConditionChip] renders. It is the *shape* of an answer, not the answer: a screen
 * maps its own domain state onto one of these five — `GOOD` for a night that is dark enough,
 * `BLOCKING` for a cloud deck that ends the plan, `UNAVAILABLE` for a condition no source could
 * answer, `NEUTRAL` for a fact that is neither good nor bad.
 *
 * Five states rather than three because the two the domain's own `ConditionState` cannot express are
 * the ones the UI must not hide: an unavailable condition is not "marginal", and a neutral fact is
 * not "good".
 */
enum class ChipState {
    GOOD,
    MARGINAL,
    BLOCKING,
    UNAVAILABLE,
    NEUTRAL,
}

/** A full radius: the pill is the one rounded shape in the identity's vocabulary. */
private val ChipShape = RoundedCornerShape(percent = 50)

/**
 * A state pill: a dot and a caller-localised word, coloured by the identity rather than by a
 * hardcoded green/amber/red.
 *
 * Each state takes a real scheme pair — `tertiaryContainer`, `primaryContainer`, `errorContainer`,
 * `secondaryContainer` — so the pill is legible on both the night and the day chrome, and red
 * night-vision mode remaps it with everything else instead of leaving one warm dot on a black screen.
 * `UNAVAILABLE` deliberately gets a *hollow* dot: the absence of a value is drawn as an absence, not
 * as one more filled severity.
 */
@Composable
fun ConditionChip(
    text: String,
    state: ChipState,
    modifier: Modifier = Modifier,
) {
    val container: Color
    val ink: Color
    when (state) {
        ChipState.GOOD -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            ink = MaterialTheme.colorScheme.onTertiaryContainer
        }

        ChipState.MARGINAL -> {
            container = MaterialTheme.colorScheme.primaryContainer
            ink = MaterialTheme.colorScheme.onPrimaryContainer
        }

        ChipState.BLOCKING -> {
            container = MaterialTheme.colorScheme.errorContainer
            ink = MaterialTheme.colorScheme.onErrorContainer
        }

        ChipState.UNAVAILABLE -> {
            container = MaterialTheme.colorScheme.surfaceContainerHighest
            ink = MaterialTheme.colorScheme.onSurfaceVariant
        }

        ChipState.NEUTRAL -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            ink = MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(container)
            .border(1.dp, ink.copy(alpha = 0.32f), ChipShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            val strokeWidth = 1.5.dp.toPx()
            when (state) {
                ChipState.UNAVAILABLE -> drawCircle(
                    color = ink,
                    radius = size.minDimension / 2f - strokeWidth / 2f,
                    style = Stroke(width = strokeWidth),
                )

                else -> drawCircle(color = ink, radius = size.minDimension / 2f)
            }
        }
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 320)
@Composable
private fun ConditionChipPreview() {
    LumintraTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(12.dp)) {
            ConditionChip(text = "Dark window open", state = ChipState.GOOD)
            Spacer(modifier = Modifier.height(8.dp))
            ConditionChip(text = "Moon up 46%", state = ChipState.MARGINAL)
            Spacer(modifier = Modifier.height(8.dp))
            ConditionChip(text = "Cloud deck", state = ChipState.BLOCKING)
            Spacer(modifier = Modifier.height(8.dp))
            ConditionChip(text = "No forecast", state = ChipState.UNAVAILABLE)
            Spacer(modifier = Modifier.height(8.dp))
            ConditionChip(text = "6 targets", state = ChipState.NEUTRAL)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun ConditionChipRedModePreview() {
    LumintraTheme(darkTheme = true, redMode = true) {
        Column(modifier = Modifier.padding(12.dp)) {
            ConditionChip(text = "Dark window open", state = ChipState.GOOD)
            Spacer(modifier = Modifier.height(8.dp))
            ConditionChip(text = "Cloud deck", state = ChipState.BLOCKING)
        }
    }
}

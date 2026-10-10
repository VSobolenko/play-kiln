package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme

/**
 * A typed empty state: a glyph, a title, a body and at most one call to action.
 *
 * An empty state that says only "nothing here" teaches nothing, so the body names what the user does
 * next and the single action is the shortest path to it. One action is a deliberate limit: two
 * buttons in an empty state are a menu, and a menu is what the screen behind it is for.
 *
 * @param iconRes a vendored `ic_ui_*` glyph for the *subject* that is empty (a telescope, a cloud, a
 *   search). When it is absent the slot's own [LumintraMark] is drawn instead, so a screen with no
 *   icon never falls back to a stock placeholder. Either way the glyph is decorative — the title and
 *   the body carry the meaning — so its content description is explicitly `null` rather than a second
 *   reading of the title.
 */
@Composable
fun LumintraEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = LumintraTheme.colors.firstLight,
                modifier = Modifier.size(32.dp),
            )
        } else {
            LumintraMark(modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onAction)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun LumintraEmptyStatePreview() {
    LumintraTheme(darkTheme = true) {
        LumintraEmptyState(
            title = "No spot yet",
            body = "Add the place you observe from and the horizon it stands behind.",
            iconRes = R.drawable.ic_ui_map_pin,
            actionLabel = "Add a spot",
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun LumintraEmptyStateMarkPreview() {
    LumintraTheme(darkTheme = true) {
        LumintraEmptyState(
            title = "Nothing to plan yet",
            body = "Choose a night and Lumintra reads the dark window for you.",
        )
    }
}

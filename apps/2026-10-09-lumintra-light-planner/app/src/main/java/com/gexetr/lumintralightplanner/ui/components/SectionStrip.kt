package com.gexetr.lumintralightplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.LumintraType

/**
 * A titled section header: a short rule in the identity's "first light" ink, the section's name in
 * the almanac's spaced voice, and a slot on the right for whatever the group can do.
 *
 * The rule is the same device the screen header uses, which is what makes a long screen read as ruled
 * groups rather than as a column of panels. The title takes `weight`, so it wraps inside the width its
 * own locale needs instead of a fixed extent a longer translation would clip; the trailing slot is
 * laid out last so an action never squeezes the heading.
 *
 * A tab row of labels is deliberately *not* this component: a strip whose style depended on English
 * label lengths would break in the longest declared locale, and how a multi-destination surface names
 * its destinations is that surface's decision, not a header's.
 */
@Composable
fun SectionStrip(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(LumintraTheme.colors.firstLight),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = LumintraType.kicker,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun SectionStripPreview() {
    LumintraTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SectionStrip(title = "Key times")
            SectionStrip(title = "Dark window")
            SectionStrip(title = "Targets", trailing = { ConditionChip(text = "6", state = ChipState.NEUTRAL) })
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F4F1, widthDp = 360)
@Composable
private fun SectionStripLightPreview() {
    LumintraTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SectionStrip(title = "Key times")
        }
    }
}

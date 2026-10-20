package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

@Composable
fun BalanceChip(balance: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = if (balance == null) stringResource(R.string.shell_balance_get_credits) else stringResource(R.string.shell_balance_count, balance)
    val purpose = stringResource(R.string.shell_balance_purpose, label)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, spring(dampingRatio = .62f, stiffness = 620f), label = "balance-press")
    Surface(onClick = onClick, interactionSource = interactions, modifier = modifier.padding(end = 4.dp).heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale }.semantics { contentDescription = purpose }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .9f), contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, PlinthPalette.Spark.copy(alpha = .5f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_plus),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = PlinthPalette.Spark,
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

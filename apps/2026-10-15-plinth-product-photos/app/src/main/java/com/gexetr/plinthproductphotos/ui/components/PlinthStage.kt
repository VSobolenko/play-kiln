package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun PlinthStage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val bench = MaterialTheme.colorScheme.surfaceContainerHigh
    val lift = MaterialTheme.colorScheme.secondaryContainer
    val page = MaterialTheme.colorScheme.surfaceContainerLow
    val edge = MaterialTheme.colorScheme.outlineVariant
    // Read inside composition: `drawWithCache` is not a composable scope, so a theme lookup there
    // is a compile error rather than a subtle bug.
    val highlightInk = MaterialTheme.colorScheme.surface.copy(alpha = .72f)
    Box(
        modifier = modifier
            .clip(shape)
            .shadow(14.dp, shape, clip = false)
            .drawWithCache {
                val field = Brush.linearGradient(
                    colors = listOf(lift.copy(alpha = .92f), bench, page),
                    start = Offset(size.width * .25f, 0f), end = Offset(size.width * .6f, size.height),
                )
                val highlight = Brush.verticalGradient(listOf(highlightInk, androidx.compose.ui.graphics.Color.Transparent), endY = size.height * .22f)
                onDrawBehind { drawRect(field); drawRect(highlight) }
            }
            .border(1.dp, edge, shape)
            .padding(20.dp),
    ) {
        content()
    }
}

@Composable
fun PlinthSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Box(modifier.clip(shape).shadow(10.dp, shape, clip = false).background(MaterialTheme.colorScheme.surfaceContainerLow).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f), shape).padding(16.dp)) { content() }
}

@Composable
fun PlinthStats(
    itemsLabel: String,
    itemsValue: String,
    packsLabel: String,
    packsValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatColumn(itemsLabel, itemsValue, Modifier.weight(1f))
        StatColumn(packsLabel, packsValue, Modifier.weight(1f))
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

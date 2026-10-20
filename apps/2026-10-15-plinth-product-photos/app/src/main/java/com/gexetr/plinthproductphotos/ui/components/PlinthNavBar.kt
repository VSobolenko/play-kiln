package com.gexetr.plinthproductphotos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gexetr.plinthproductphotos.R
import com.gexetr.plinthproductphotos.ui.theme.PlinthPalette

internal enum class PlinthGlassEdge { TOP, BOTTOM, END }

@Composable
internal fun Modifier.plinthTonalGlass(edge: PlinthGlassEdge): Modifier {
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    val accent = MaterialTheme.colorScheme.primaryContainer
    val shadow = MaterialTheme.colorScheme.onSurface
    val rim = MaterialTheme.colorScheme.outlineVariant
    val highlight = MaterialTheme.colorScheme.surfaceBright
    return drawWithCache {
        val wash = Brush.horizontalGradient(listOf(base.copy(.92f), base.copy(.78f), accent.copy(.30f)))
        val depth = when (edge) {
            PlinthGlassEdge.TOP -> Brush.verticalGradient(listOf(shadow.copy(.18f), Color.Transparent), endY = size.height * .46f)
            PlinthGlassEdge.BOTTOM -> Brush.verticalGradient(listOf(Color.Transparent, shadow.copy(.16f)), startY = size.height * .55f)
            PlinthGlassEdge.END -> Brush.horizontalGradient(listOf(Color.Transparent, shadow.copy(.16f)), startX = size.width * .55f)
        }
        onDrawBehind {
            drawRect(base.copy(.88f)); drawRect(wash); drawRect(depth)
            val stroke = 1.dp.toPx()
            when (edge) {
                PlinthGlassEdge.TOP -> drawLine(Brush.horizontalGradient(listOf(rim.copy(.72f), highlight.copy(.92f), rim.copy(.12f))), Offset.Zero, Offset(size.width, 0f), stroke)
                PlinthGlassEdge.BOTTOM -> drawLine(Brush.horizontalGradient(listOf(rim.copy(.72f), highlight.copy(.92f), rim.copy(.12f))), Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                PlinthGlassEdge.END -> drawLine(rim.copy(.75f), Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
            }
        }
    }
}

/**
 * The bottom bar's selection pill lives *inside* each item rather than being one absolutely
 * offset box. The previous version moved a single 32 dp capsule with `offset(x = itemWidth * index)`
 * at the top of the bar: it was never vertically aligned with the icon it was meant to mark, its
 * fractional pixel offset softened both edges, and it collapsed to 11 dp on a device whose bar
 * measured shorter than its declared minimum — the owner's "обводка поехала". A pill that is a
 * child of the item it marks cannot drift: it is placed by the layout, animates colour and scale
 * in place, and stays aligned at every font scale and locale. Nothing here reads or writes an
 * absolute position, so there is no geometry left to get wrong.
 */
@Composable
fun PlinthBottomNavBar(selected: PlinthSection, onSelect: (PlinthSection) -> Unit, onCreate: () -> Unit) {
    val sections = listOf(PlinthSection.STUDIO, PlinthSection.LIBRARY, PlinthSection.PROFILE, PlinthSection.MORE)
    Surface(
        modifier = Modifier.fillMaxWidth().plinthTonalGlass(PlinthGlassEdge.TOP),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {            sections.take(2).forEach { BottomNavItem(it, it == selected, { onSelect(it) }, Modifier.weight(1f)) }
            BottomNavCreateItem(onCreate, Modifier.weight(1f))
            sections.drop(2).forEach { BottomNavItem(it, it == selected, { onSelect(it) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
fun PlinthNavigationRail(selected: PlinthSection, onSelect: (PlinthSection) -> Unit, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val sections = listOf(PlinthSection.STUDIO, PlinthSection.LIBRARY, PlinthSection.PROFILE, PlinthSection.MORE)
    Surface(
        modifier = modifier.fillMaxHeight().plinthTonalGlass(PlinthGlassEdge.END),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.width(88.dp).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.take(2).forEach { RailItem(it, it == selected, { onSelect(it) }) }
            RailCreateItem(onCreate)
            sections.drop(2).forEach { RailItem(it, it == selected, { onSelect(it) }) }
        }
    }
}

@Composable private fun BottomNavItem(section: PlinthSection, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val pill by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(.92f) else Color.Transparent,
        animationSpec = spring(dampingRatio = .82f, stiffness = 620f),
        label = "nav-pill",
    )
    val glyphTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = .82f, stiffness = 620f),
        label = "nav-glyph",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = .82f, stiffness = 620f),
        label = "nav-label",
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1f else .84f,
        animationSpec = spring(dampingRatio = .8f, stiffness = 620f),
        label = "nav-pill-scale",
    )
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .graphicsLayer { scaleX = pillScale; scaleY = pillScale }
                .background(pill, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(sectionIcon(section)),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = glyphTint,
            )
        }
        Text(
            stringResource(sectionLabel(section)),
            color = labelTint,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable private fun BottomNavCreateItem(onClick: () -> Unit, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }; val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .91f else 1f, spring(dampingRatio = .58f, stiffness = 680f), label = "create-press")
    val createLabel = stringResource(R.string.shell_create)
    Column(
        modifier = modifier.heightIn(min = 64.dp).selectable(selected = false, onClick = onClick, role = Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(Brush.linearGradient(listOf(PlinthPalette.Spark, PlinthPalette.Amber)), CircleShape)
                .padding(2.dp),
            Alignment.Center,
        ) {
            Surface(
                onClick = onClick,
                interactionSource = source,
                modifier = Modifier.fillMaxWidth().fillMaxHeight().semantics { contentDescription = createLabel },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_plus),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Text(stringResource(R.string.shell_create_label), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable private fun RailItem(section: PlinthSection, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 64.dp).background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(.88f) else Color.Transparent, RoundedCornerShape(18.dp)).selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(sectionIcon(section)),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(sectionLabel(section)), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable private fun RailCreateItem(onClick: () -> Unit) { BottomNavCreateItem(onClick, Modifier.width(72.dp)) }
internal fun sectionLabel(section: PlinthSection) = when (section) { PlinthSection.STUDIO -> R.string.shell_studio; PlinthSection.LIBRARY -> R.string.shell_library; PlinthSection.PROFILE -> R.string.shell_profile; PlinthSection.MORE -> R.string.shell_more }
internal fun sectionIcon(section: PlinthSection) = when (section) { PlinthSection.STUDIO -> R.drawable.ic_lucide_image; PlinthSection.LIBRARY -> R.drawable.ic_lucide_search; PlinthSection.PROFILE -> R.drawable.ic_lucide_user; PlinthSection.MORE -> R.drawable.ic_lucide_settings }

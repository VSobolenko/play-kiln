package com.gexetr.coverkit.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gexetr.coverkit.R
import com.gexetr.coverkit.domain.CoverProject
import com.gexetr.coverkit.domain.Destination
import com.gexetr.coverkit.domain.Layer
import com.gexetr.coverkit.domain.TemplateBackdrop
import com.gexetr.coverkit.ui.theme.Cyan
import com.gexetr.coverkit.ui.theme.Ink
import com.gexetr.coverkit.ui.theme.InkLift
import com.gexetr.coverkit.ui.theme.InkSoft
import com.gexetr.coverkit.ui.theme.Paper
import kotlin.math.max

private val CellShape = RoundedCornerShape(18.dp)

/**
 * Bottom chrome clearance under icon+label rows.
 * A40 reports navigationBars as 0 while soft keys still cover the last ~48dp;
 * always reserve at least that so labels stay readable (owner 2026-10-21).
 */
@Composable
fun chromeNavBottom(): Dp {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Hard floor — measured on A40: inset 0 still needs ~48dp under the 72dp tool row.
    return if (inset > 48.dp) inset else 48.dp
}

/** Total height of StudioBottomBar / EditorRail chrome (tools + nav floor). */
val StudioChromeHeight: Dp = 72.dp + 48.dp

@Composable
fun StudioScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // Column layout (not overlay): overlay chrome was compressed on A40 so cards
    // sat under the bar and labels had no nav floor (owner 2026-10-21).
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                StudioIconButton(R.drawable.ic_arrow_left, stringResource(R.string.action_back), onBack)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Paper,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        if (bottom != null) {
            bottom()
        } else {
            Spacer(Modifier.height(chromeNavBottom()))
        }
    }
}

/**
 * Extra scroll clearance under the last card so a ~208.dp destination cell can
 * scroll fully into the viewport (owner: Pinterest/LinkedIn trapped under chrome).
 */
@Composable
fun studioScrollBottomPadding(): Dp = 96.dp

/**
 * Fixed 72dp icon+label row, then a same-colour spacer for the system nav.
 * Inset must not share the Row with the icons — that crushed labels to ~4px
 * and looked like a mysterious gap under the tools (owner 2026-10-21).
 */
@Composable
fun StudioBottomBar(onProjects: () -> Unit, onBrand: () -> Unit, onSettings: () -> Unit) {
    Surface(color = InkSoft, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().height(StudioChromeHeight)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudioNavItem(Modifier.weight(1f), R.drawable.ic_folder, stringResource(R.string.nav_projects), onProjects)
                StudioNavItem(Modifier.weight(1f), R.drawable.ic_palette, stringResource(R.string.nav_brand), onBrand)
                StudioNavItem(Modifier.weight(1f), R.drawable.ic_settings, stringResource(R.string.settings_title), onSettings)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StudioNavItem(modifier: Modifier, @DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = Cyan,
            modifier = Modifier.size(22.dp),
        )
        Text(label, color = Paper, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StudioIconButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(painter = painterResource(icon), contentDescription = label, tint = Cyan, modifier = Modifier.size(22.dp))
    }
}

/** A fixed card cell; the frame, not the grid row, expresses the destination ratio. */
@Composable
fun AspectPreviewCard(destination: Destination, onClick: () -> Unit, minHeight: Dp = 208.dp) {
    val ratio = destination.widthPx.toFloat() / destination.heightPx
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).clickable(onClick = onClick),
        shape = CellShape,
        color = InkSoft,
        border = BorderStroke(1.dp, InkLift),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            LetterboxedFrame(ratio = ratio, modifier = Modifier.weight(1f)) { DestinationSampleArt(destination.id) }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(destination.labelRes), color = Paper, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.dest_size_format, destination.widthPx, destination.heightPx), color = Cyan, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TemplateThumb(templateName: String, project: CoverProject, locked: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ProjectArt(project, Modifier.fillMaxWidth().aspectRatio(1.05f))
        Text(templateName, color = Paper, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        if (locked) Text(stringResource(R.string.template_locked), color = Cyan, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun BlankTemplateThumb(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.05f).clip(RoundedCornerShape(14.dp)).background(InkSoft).border(BorderStroke(1.dp, Cyan), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_photo), contentDescription = stringResource(R.string.nav_photo), tint = Cyan, modifier = Modifier.size(34.dp)) }
        Text(stringResource(R.string.template_blank_photo), color = Paper, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
fun ProjectThumb(
    project: CoverProject,
    destinationLabel: String,
    updatedLabel: String?,
    onOpen: () -> Unit,
    onActions: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Box {
            ProjectArt(project, Modifier.fillMaxWidth().aspectRatio(1.05f))
            TextButton(
                onClick = onActions,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Text(stringResource(R.string.projects_more), color = Paper)
            }
        }
        Text(project.name, color = Paper, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp))
        Text(destinationLabel, color = Cyan, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        updatedLabel?.let {
            Text(it, color = Paper.copy(alpha = .68f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProjectArt(project: CoverProject, modifier: Modifier = Modifier) {
    val headline = project.layers.filterIsInstance<Layer.TextLayer>().firstOrNull()
    val photo = project.layers.filterIsInstance<Layer.PhotoLayer>().firstOrNull()
    val style = TemplateBackdrop.fromIndex(project.backdropStyle)
    val color = photo?.colorArgb ?: 0xFF24596A.toInt()
    val bmp = remember(project.id, color, style) {
        TemplateBackdrop.render(480, 480, color, style)
    }
    Box(modifier.clip(RoundedCornerShape(14.dp))) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (headline != null) {
            Text(
                text = headline.text,
                color = Paper,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun LetterboxedFrame(ratio: Float, modifier: Modifier = Modifier, art: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink)) {
        Box(
            Modifier.align(Alignment.Center).fillMaxHeight().widthIn(max = 150.dp).aspectRatio(ratio, matchHeightConstraintsFirst = true).border(1.dp, InkLift, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)),
        ) { art() }
    }
}

@Composable
private fun DestinationSampleArt(id: String) {
    Canvas(Modifier.fillMaxSize()) {
        val accent = when (id) {
            "yt_thumb" -> Color(0xFFE05A62)
            "ig_post" -> Color(0xFFFFA45C)
            "tiktok_cover" -> Cyan
            else -> Color(0xFF9376E8)
        }
        drawRect(Brush.linearGradient(listOf(InkLift, Ink), Offset.Zero, Offset(size.width, size.height)))
        drawCircle(accent.copy(alpha = .8f), size.minDimension * .27f, Offset(size.width * .72f, size.height * .26f))
        drawRect(Paper, Offset(size.width * .12f, size.height * .39f), Size(size.width * .48f, max(5f, size.height * .10f)))
        drawRect(Paper.copy(alpha = .64f), Offset(size.width * .12f, size.height * .54f), Size(size.width * .34f, max(4f, size.height * .045f)))
        drawRect(accent, Offset(size.width * .12f, size.height * .73f), Size(size.width * .27f, max(7f, size.height * .075f)))
    }
}

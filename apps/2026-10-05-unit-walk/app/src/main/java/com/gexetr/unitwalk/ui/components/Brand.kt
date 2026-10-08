package com.gexetr.unitwalk.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.unitwalk.R
import com.gexetr.unitwalk.ui.theme.HeroCorner
import com.gexetr.unitwalk.ui.theme.PillCorner
import com.gexetr.unitwalk.ui.theme.SheetCorner
import com.gexetr.unitwalk.ui.theme.TileCorner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/*
 * UnitWalk's component vocabulary: the shapes this app makes, rather than the ones a library makes.
 *
 * Every member here encodes a decision the app makes more than once — a room's condition, a unit's
 * state, a walk that is finished, a photo that has not been taken yet — which is what earns a
 * component its place. None of them wraps `Card`.
 *
 * Two rules from `.cursor/rules/visual-identity.mdc` are load-bearing in this file and are stated at
 * the site where they are honoured:
 *
 *   - **A state is carried by a word and a shape, never by colour alone** ([StatusPill]). Colour is
 *     the third signal here, not the first: the label is always rendered, and the pill's silhouette
 *     differs per kind, so the state survives a screenshot in greyscale, a colour-blind reader and a
 *     locale whose words are longer than English's.
 *   - **General-purpose iconography is a library's** — the add affordance and the lock in this file
 *     are the vendored set's glyphs (`R.drawable.ic_add`, `R.drawable.ic_lock`), tinted from the
 *     theme and never colouring themselves. The two exceptions are the *state marks* inside
 *     [StatusPill] that the set has no honest glyph for: an attention triangle and an undetermined
 *     disc, drawn as geometry because their meaning is this app's own ledger vocabulary rather than
 *     a word any icon set defines.
 *
 * The signature of every function below is frozen: other workers write against it and must compile.
 */

/** How a walk's condition reads at a glance. The order is the app's own, from good to blocking. */
enum class PillKind { OK, WARN, NEUTRAL, DANGER }

// ---------------------------------------------------------------------------------------------
// The mark
// ---------------------------------------------------------------------------------------------

/*
 * The mark's geometry is the registered launcher mark (`mark-unit-plan` in
 * `scripts/lib/icon_design.py`) drawn in a 108x108 viewport, and every constant below is that
 * artwork's own measured value divided by 108 — so the in-app mark and the launcher icon are one
 * silhouette, not two drawings that resemble each other.
 *
 *   - the room's walls are one closed stroke of width 5.06, whose round join is the corner radius;
 *   - the walls sit on the centreline rectangle x 31.86..76.14, y 36.72..71.28 (the room is wider
 *     than it is deep, because a unit in plan is a rectangle and not a page);
 *   - the walk's tick is the three-point polyline through the cap centres (45.14, 53.79),
 *     (50.77, 60.05) and (62.86, 46.43).
 */
private const val MarkViewport = 108f
private const val MarkStroke = 5.06f / MarkViewport
private const val MarkFrameLeft = 31.86f / MarkViewport
private const val MarkFrameTop = 36.72f / MarkViewport
private const val MarkFrameRight = 76.14f / MarkViewport
private const val MarkFrameBottom = 71.28f / MarkViewport
private const val MarkFrameRadius = 2.53f / MarkViewport
private const val MarkTickStartX = 45.14f / MarkViewport
private const val MarkTickStartY = 53.79f / MarkViewport
private const val MarkTickCornerX = 50.77f / MarkViewport
private const val MarkTickCornerY = 60.05f / MarkViewport
private const val MarkTickEndX = 62.86f / MarkViewport
private const val MarkTickEndY = 46.43f / MarkViewport

/**
 * The slot's own mark: a rental unit in plan, with the walk's tick inside its walls.
 *
 * It is drawn here rather than loaded from a drawable so it can take [tint] from whatever it is
 * placed on — a hero panel and a strip pill need different colours from the same silhouette — and so
 * it carries no colour of its own. It is decorative by default: it has no accessible name, because
 * wherever it appears it either stands beside a label or replaces one whose name the enclosing
 * control already carries ([SectionStrip]).
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val side = size.minDimension
        if (side <= 0f) return@Canvas
        translate(left = (size.width - side) / 2f, top = (size.height - side) / 2f) {
            val walls = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = side * MarkFrameLeft,
                            top = side * MarkFrameTop,
                            right = side * MarkFrameRight,
                            bottom = side * MarkFrameBottom,
                        ),
                        cornerRadius = CornerRadius(
                            x = side * MarkFrameRadius,
                            y = side * MarkFrameRadius,
                        ),
                    ),
                )
            }
            val wallsStyle = Stroke(
                width = side * MarkStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            drawPath(walls, color = tint, style = wallsStyle)

            val tick = Path().apply {
                moveTo(side * MarkTickStartX, side * MarkTickStartY)
                lineTo(side * MarkTickCornerX, side * MarkTickCornerY)
                lineTo(side * MarkTickEndX, side * MarkTickEndY)
            }
            drawPath(tick, color = tint, style = wallsStyle)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------------------------

/** The horizontal gap between two strip items. */
private val StripGap = 8.dp

/** A labelled pill's horizontal padding; the mark-only pill uses [StripMarkPadding] instead. */
private val StripLabelPadding = 16.dp

/** A mark-only pill's padding: enough to hold the mark, small enough to leave the 48dp target. */
private val StripMarkPadding = 12.dp

/** The mark inside a strip pill, and the glyph inside a status pill. */
private val MarkPillSize = 22.dp

/**
 * The section strip: the app's top-level sections as labelled pills.
 *
 * **Why an inactive item can be the mark alone.** Not every section of the strip fits on a phone as
 * a labelled pill, and whether it does is a property of the *locale* and not of the layout: the same
 * strip that fits in English overflows in a language whose words are longer, and a fixed-width pill or
 * an ellipsised label is the defect that only appears after translation. So the strip **measures the
 * real strings** with [rememberTextMeasurer] against the width it was actually given, and when they
 * do not fit it drops the words from the inactive items and keeps the mark — which is the app's own
 * identity and needs no translation — while the selected item keeps its word. The selected item is
 * never reduced to a mark, so the current section is always named. Screen readers still get the name
 * of a mark-only item, because the control carries it as its accessible description.
 */
@Composable
fun SectionStrip(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val textStyle = MaterialTheme.typography.labelLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val labelWidths = items.map { measurer.measure(it, textStyle).size.width }
        val labelledWidth = with(density) {
            val padding = (StripLabelPadding * 2).roundToPx()
            val gap = StripGap.roundToPx()
            (labelWidths.sum() + padding * items.size + gap * (items.size - 1)).toDp()
        }
        // `maxWidth` is `Dp.Infinity` inside a scrollable parent, and `labelledWidth > Dp.Infinity`
        // is false, so an unbounded strip keeps its words rather than collapsing for no reason.
        val compact = labelledWidth > maxWidth

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(StripGap),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val showsLabel = selected || !compact
                SectionPill(
                    label = item,
                    selected = selected,
                    showsLabel = showsLabel,
                    style = textStyle,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

/** One pill of the strip. `role = Role.Tab` is what it is: one section of a set. */
@Composable
private fun SectionPill(
    label: String,
    selected: Boolean,
    showsLabel: Boolean,
    style: TextStyle,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(PillCorner)
            .background(container)
            .then(
                if (showsLabel) {
                    Modifier
                } else {
                    // The word is not drawn, so the control must still be named out loud.
                    Modifier.semantics { contentDescription = label }
                },
            )
            .clickable(onClickLabel = label, role = Role.Tab, onClick = onClick)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(
                horizontal = if (showsLabel) StripLabelPadding else StripMarkPadding,
                vertical = 6.dp,
            ),
    ) {
        if (showsLabel) {
            Text(
                text = label,
                style = style,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            BrandMark(modifier = Modifier.size(MarkPillSize), tint = content)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Data display
// ---------------------------------------------------------------------------------------------

/**
 * A single measured number with its name. The amber rule above the value is the identity's accent,
 * and it is the only saturated thing on a tile — the value and its label are ink and secondary ink,
 * so a screen full of tiles does not turn into a screen full of colour.
 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = TileCorner,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(PillCorner)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A hero panel's padding, and the watermark's size. */
private val HeroPadding = 20.dp
private val HeroMarkSize = 46.dp

/**
 * The screen's headline panel: a tonal field with the walk's own summary on it.
 *
 * The field is a gradient across the three scheme colours rather than a flat rectangle, because depth
 * is the difference between a panel and a box: the eye gets a light direction from it. The content
 * colour is `onPrimary`, and every stop of the gradient was measured against it — 10.33:1 / 7.34:1 /
 * 5.60:1 across the light scheme's `primary` / `secondary` / `tertiary` stops and 8.27:1 / 8.43:1 /
 * 8.59:1 across the dark scheme's, so the worst any text on this panel can meet is 5.60:1 and the
 * text is legible wherever the gradient happens to fall behind it.
 */
@Composable
fun HeroPanel(
    title: String,
    subtitle: String,
    stat: String,
    statLabel: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val content = scheme.onPrimary
    Box(
        modifier = modifier
            .clip(HeroCorner)
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary, scheme.tertiary)))
            .padding(HeroPadding),
    ) {
        BrandMark(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(HeroMarkSize),
            tint = content.copy(alpha = 0.45f),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stat,
                    style = MaterialTheme.typography.displaySmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------------------------

/** The glyph a status pill's state mark is drawn at. */
private val StatusMarkSize = 16.dp

/** A status pill's padding. */
private val StatusPaddingHorizontal = 12.dp
private val StatusPaddingVertical = 8.dp

/**
 * A unit's or a walk's state, stated three ways: a word, a silhouette and a colour.
 *
 * The word is always drawn, so the state never depends on a reader telling amber from teal; the
 * silhouette differs per kind (round, one cut corner, plain rectangle, two opposed cut corners), so
 * the state survives greyscale and prints; and the colour pair for each kind was measured at WCAG AA
 * or better in both schemes — 12.22:1 / 12.66:1 / 6.42:1 / 13.09:1 in light and 7.96:1 / 9.01:1 /
 * 7.09:1 / 7.24:1 in dark for `OK` / `WARN` / `NEUTRAL` / `DANGER`, so the worst of the eight is
 * 6.42:1, the light scheme's neutral pair. Colour is the fastest signal here, and the least
 * load-bearing one.
 */
@Composable
fun StatusPill(label: String, kind: PillKind, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container = when (kind) {
        PillKind.OK -> scheme.secondaryContainer
        PillKind.WARN -> scheme.tertiaryContainer
        PillKind.NEUTRAL -> scheme.surfaceVariant
        PillKind.DANGER -> scheme.errorContainer
    }
    val content = when (kind) {
        PillKind.OK -> scheme.onSecondaryContainer
        PillKind.WARN -> scheme.onTertiaryContainer
        PillKind.NEUTRAL -> scheme.onSurfaceVariant
        PillKind.DANGER -> scheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = statusShape(kind),
        color = container,
        contentColor = content,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = StatusPaddingHorizontal,
                vertical = StatusPaddingVertical,
            ),
        ) {
            StatusMark(kind = kind, tint = content)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One silhouette per kind, so the shape alone separates the four states. */
private fun statusShape(kind: PillKind): Shape = when (kind) {
    PillKind.OK -> PillCorner
    PillKind.WARN -> CutCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomEnd = 14.dp,
        bottomStart = 4.dp,
    )
    PillKind.NEUTRAL -> TileCorner
    PillKind.DANGER -> CutCornerShape(
        topStart = 16.dp,
        topEnd = 4.dp,
        bottomEnd = 16.dp,
        bottomStart = 4.dp,
    )
}

/**
 * The state mark. `OK` and `DANGER` are the vendored set's own check and cross — the two meanings a
 * professional set does define — and are passed `contentDescription = null` because the pill's label
 * beside them already names the state. `WARN` and `NEUTRAL` are geometry, because no icon set carries
 * *this app's* ledger vocabulary (attention, undetermined) and bending a library glyph to mean one
 * would be the defect rather than the fix.
 */
@Composable
private fun StatusMark(kind: PillKind, tint: Color) {
    when (kind) {
        PillKind.OK -> Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            modifier = Modifier.size(StatusMarkSize),
            tint = tint,
        )
        PillKind.DANGER -> Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null,
            modifier = Modifier.size(StatusMarkSize),
            tint = tint,
        )
        PillKind.WARN -> Canvas(modifier = Modifier.size(StatusMarkSize)) {
            val side = size.minDimension
            val triangle = Path().apply {
                moveTo(side * 0.5f, side * 0.12f)
                lineTo(side * 0.94f, side * 0.86f)
                lineTo(side * 0.06f, side * 0.86f)
                close()
            }
            drawPath(
                path = triangle,
                color = tint,
                style = Stroke(width = side * 0.16f, join = StrokeJoin.Round),
            )
        }
        PillKind.NEUTRAL -> Canvas(modifier = Modifier.size(StatusMarkSize)) {
            val side = size.minDimension
            drawCircle(
                color = tint,
                radius = side * 0.26f,
                center = Offset(side / 2f, side / 2f),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Photos
// ---------------------------------------------------------------------------------------------

/** The edge of one thumbnail, and of the add affordance beside it. */
private val ThumbSize = 84.dp

/** The longest edge a decoded thumbnail keeps in memory; the file itself is never loaded whole. */
private const val ThumbDecodePx = 320

/** The label a photo carries: its file's own name, without the directory or the extension. */
private fun photoLabel(fileName: String): String = fileName
    .substringAfterLast('/')
    .substringAfterLast('\\')
    .substringBeforeLast('.')
    .ifBlank { fileName }

/** A photo is named by the caller either in full or relative to the app's private files directory. */
private fun resolvePhoto(directory: File, fileName: String): File {
    val direct = File(fileName)
    return if (direct.isFile) direct else File(directory, fileName)
}

/**
 * A walk's photos, as a strip of thumbnails with a labelled way to add the next one.
 *
 * Thumbnails are decoded from the app's own files off the main thread and **subsampled** — a 320px
 * longest edge is all a thumbnail the height of a fingertip can show, and decoding a full-resolution
 * photo per tile is how a screen full of rooms runs out of memory. A file that cannot be read is not
 * an exception here: it renders as the mark, because a missing thumbnail is a state a walker will
 * meet and must not crash on.
 *
 * Every tile is a 48dp-or-larger target, and every image carries a name: the file's own. The add
 * affordance is tinted from the theme rather than from `LocalContentColor`, so it reads on the panel
 * it is placed on whatever the enclosing layout provides.
 */
@Composable
fun PhotoThumbRow(
    fileNames: List<String>,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    addLabel: String,
    modifier: Modifier = Modifier,
) {
    val directory = LocalContext.current.filesDir
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fileNames.forEach { fileName ->
            Thumbnail(fileName = fileName, directory = directory, onOpen = onOpen)
        }
        AddThumbnail(label = addLabel, onAdd = onAdd)
    }
}

@Composable
private fun Thumbnail(fileName: String, directory: File, onOpen: (String) -> Unit) {
    val label = photoLabel(fileName)
    val photo by produceState<ImageBitmap?>(initialValue = null, fileName, directory) {
        value = withContext(Dispatchers.IO) {
            decodeThumbnail(resolvePhoto(directory, fileName), ThumbDecodePx)
        }
    }
    Box(
        modifier = Modifier
            .size(ThumbSize)
            .clip(TileCorner)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = label, onClick = { onOpen(fileName) }),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = photo
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            BrandMark(
                modifier = Modifier.size(MarkPillSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddThumbnail(label: String, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ThumbSize)
            .clip(TileCorner)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = TileCorner)
            .clickable(onClickLabel = label, onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

/** A subsampled decode, or `null` when the file is gone, unreadable or not an image. */
private fun decodeThumbnail(file: File, longestEdge: Int): ImageBitmap? {
    if (!file.isFile) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= longestEdge || bounds.outHeight / (sample * 2) >= longestEdge) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
    } catch (_: OutOfMemoryError) {
        null
    }
}

// ---------------------------------------------------------------------------------------------
// Emptiness and locks
// ---------------------------------------------------------------------------------------------

/**
 * The state of having nothing yet: the mark, a title and one sentence about what to do.
 *
 * It is a room of its own rather than a grey sentence in the middle of a list, because "you have no
 * units yet" is the first screen a new landlord sees and the app's only chance to say what it is for.
 */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = SheetCorner,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(
                modifier = Modifier.size(HeroMarkSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A capability that is not unlocked: what it is, and the one action that changes it.
 *
 * The lock is the vendored set's own glyph, tinted from the theme; the action is a `TextButton`
 * rather than a bare `Text` so the target is the platform's and never a text-sized tap area, and it
 * is forced to 48dp because Material's own minimum is 40dp.
 */
@Composable
fun LockedRow(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = SheetCorner,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onAction,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

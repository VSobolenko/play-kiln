package com.gexetr.teachergradebook.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gexetr.teachergradebook.ui.theme.markBookColors
import com.gexetr.teachergradebook.R

/**
 * The shared visual language, in one place.
 *
 * Before this file each screen built its own `Scaffold`, its own title, and its own idea of padding,
 * which is how the same app ended up with a row whose text ran into the right edge and cards that
 * were flush in one screen and inset in another. The fix is not to correct each padding literal; it
 * is to give the screens one shell to opt into, so an inconsistency becomes a thing you would have
 * to deliberately write.
 *
 * The marks are drawn rather than imported. Material's icon set is not on the classpath, and adding
 * the extended set would ship a few thousand vectors to use six - and every grade book on Play uses
 * the same ones, which is the opposite of the point.
 */

/** Horizontal rhythm for every screen. One number, so screens cannot disagree. */
val ScreenGutter = 20.dp

/** Tone of a pill, tile or mark. Named for meaning, mapped to colour in one place. */
enum class Tone { Neutral, Brand, Positive, Attention, Danger }

@Composable
private fun toneColors(tone: Tone): Pair<Color, Color> = when (tone) {
    Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    Tone.Brand -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primaryContainer
    Tone.Positive -> markBookColors.present to markBookColors.presentContainer
    Tone.Attention -> markBookColors.late to markBookColors.lateContainer
    Tone.Danger -> markBookColors.absent to markBookColors.absentContainer
}

/**
 * The one shell a screen uses.
 *
 * `contentPadding` is applied by the shell, not by each caller, so a screen cannot forget it - the
 * defect that put "New class" and the Options menu hard against the window edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenShell(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.nav_back)) }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = floatingActionButton,
    ) { inner ->
        content(inner)
    }
}

/**
 * The header a screen leads with: a gradient panel that states the one thing the screen is for.
 *
 * This exists because the first build opened every screen with a bare toolbar and a list, which is
 * the "just tabs, just buttons" reading the owner gave. A panel spends the top of the screen on
 * orientation instead of decoration: which class, how many students, and where the marking stands.
 */
@Composable
fun HeroPanel(
    eyebrow: String,
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    stats: List<HeroStat> = emptyList(),
) {
    val colors = markBookColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onHeroMuted,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onHero,
                fontWeight = FontWeight.Bold,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onHeroMuted,
                )
            }
            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.forEach { stat ->
                        HeroStatCell(stat, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** One number in a [HeroPanel]. */
data class HeroStat(val label: String, val value: String)

@Composable
private fun HeroStatCell(stat: HeroStat, modifier: Modifier = Modifier) {
    val colors = markBookColors
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = stat.value,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onHero,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onHeroMuted,
        )
    }
}

/** A pill for a state that has a name: present, late, missing, exempt, unmarked. */
@Composable
fun StatusPill(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
) {
    val (content, container) = toneColors(tone)
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * The overall grade, shown the way a teacher reads it: a big number, with the letter band under it.
 *
 * The letter is derived from the class's own scale, not hard-coded, so a school using a different
 * band set does not get a wrong letter printed next to a right percentage.
 */
@Composable
fun GradeDisplay(
    percentText: String,
    modifier: Modifier = Modifier,
    letter: String? = null,
    caption: String? = null,
    tone: Tone = Tone.Brand,
) {
    val (content, container) = toneColors(tone)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.displaySmall,
                color = content,
                fontWeight = FontWeight.Bold,
            )
            if (letter != null) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleLarge,
                    color = content.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                )
            }
        }
        caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A labelled statistic, for a dashboard row. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
) {
    val (content, container) = toneColors(tone)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = content,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** A card with the app's own elevation, so no screen has to remember which of the two exists. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
    content: @Composable () -> Unit,
) {
    val (_, container) = toneColors(tone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/**
 * The section switcher, replacing the plain `TabRow`.
 *
 * A `TabRow` draws text with an underline, which is what made the classroom read as "just tabs". This
 * is a scrollable row of pills with a mark and an animated container, so the current section is
 * legible at a glance and the row survives long section names in German or French.
 */
enum class SectionMark { Roster, Grades, Attendance, Notes, Seating, Reports, Setup }

@Composable
fun SectionStrip(
    sections: List<Pair<SectionMark, String>>,
    selected: SectionMark,
    onSelect: (SectionMark) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only the selected pill draws its label, so it is also the widest pill in the row. Selecting a
    // section near the end would otherwise push that label past the screen edge and clip the very
    // words this strip exists to show, so the active pill asks to be brought into view. The request
    // is a no-op when the pill already fits.
    val activeRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { activeRequester.bringIntoView() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sections.forEach { (mark, label) ->
            val active = mark == selected
            SectionPill(
                mark = mark,
                label = label,
                active = active,
                onClick = { onSelect(mark) },
                modifier = if (active) Modifier.bringIntoViewRequester(activeRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun SectionPill(
    mark: SectionMark,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "section-container",
    )
    val content by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "section-content",
    )

    Surface(
        onClick = onClick,
        // Seven labelled pills cannot fit the width of a phone, and the longest translations are the
        // ones that must not wrap or truncate. So an inactive pill is its mark alone and only the
        // current section names itself. The name then has no text node to travel on, which would
        // leave a screen reader announcing an unlabelled button, so it rides on the button here.
        modifier = modifier.semantics {
            if (!active) contentDescription = label
        },
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (active) 14.dp else 12.dp,
                end = if (active) 14.dp else 12.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Mark(mark = mark, tint = content, size = 16.dp)
            if (active) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The section marks, drawn from rectangles and lines only.
 *
 * Deliberately geometric: a shape built from `drawRoundRect` calls cannot be subtly wrong the way a
 * hand-written bezier path can, and at 16dp they read more clearly than a detailed pictogram. Each
 * mark is a distinct silhouette - bars, a grid, a tick, ruled lines - so the sections are
 * distinguishable in greyscale and for a colour-blind reader.
 */
@Composable
fun Mark(mark: SectionMark, tint: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f
        val r = CornerRadius(stroke, stroke)

        when (mark) {
            // Two heads and shoulders: the roster.
            SectionMark.Roster -> {
                drawCircle(tint, radius = w * 0.17f, center = Offset(w * 0.34f, h * 0.28f))
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.52f),
                    size = Size(w * 0.44f, h * 0.30f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                )
                drawCircle(tint.copy(alpha = 0.6f), radius = w * 0.12f, center = Offset(w * 0.74f, h * 0.33f))
                drawRoundRect(
                    color = tint.copy(alpha = 0.6f),
                    topLeft = Offset(w * 0.60f, h * 0.56f),
                    size = Size(w * 0.30f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                )
            }
            // Three ascending bars: the grades.
            SectionMark.Grades -> {
                val bars = listOf(0.42f, 0.64f, 0.86f)
                bars.forEachIndexed { index, height ->
                    val x = w * (0.14f + index * 0.30f)
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(x, h * (1f - height)),
                        size = Size(w * 0.18f, h * height),
                        cornerRadius = r,
                    )
                }
            }
            // A box with a tick: the register.
            SectionMark.Attendance -> {
                drawRoundRect(
                    color = tint.copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.80f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.32f, h * 0.54f),
                    end = Offset(w * 0.45f, h * 0.67f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.45f, h * 0.67f),
                    end = Offset(w * 0.70f, h * 0.38f),
                    strokeWidth = stroke * 1.2f,
                    cap = StrokeCap.Round,
                )
            }
            // Ruled lines with a pencil nib: the notes.
            SectionMark.Notes -> {
                listOf(0.24f, 0.46f, 0.68f).forEach { y ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.12f, h * y),
                        size = Size(w * (if (y == 0.68f) 0.44f else 0.72f), stroke),
                        cornerRadius = r,
                    )
                }
                drawCircle(tint.copy(alpha = 0.6f), radius = w * 0.14f, center = Offset(w * 0.78f, h * 0.76f))
            }
            // A two-by-two grid of desks: the seating plan.
            SectionMark.Seating -> {
                listOf(0f, 1f).forEach { col ->
                    listOf(0f, 1f).forEach { row ->
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(w * (0.14f + col * 0.42f), h * (0.18f + row * 0.40f)),
                            size = Size(w * 0.30f, h * 0.28f),
                            cornerRadius = r,
                        )
                    }
                }
            }
            // A page with a rising line on it: the reports.
            SectionMark.Reports -> {
                drawRoundRect(
                    color = tint.copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.16f, h * 0.10f),
                    size = Size(w * 0.68f, h * 0.80f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.66f),
                    end = Offset(w * 0.44f, h * 0.52f),
                    strokeWidth = stroke * 1.1f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.44f, h * 0.52f),
                    end = Offset(w * 0.68f, h * 0.34f),
                    strokeWidth = stroke * 1.1f,
                    cap = StrokeCap.Round,
                )
            }
            // A slider: the class's own rules.
            SectionMark.Setup -> {
                drawRoundRect(
                    color = tint.copy(alpha = 0.5f),
                    topLeft = Offset(w * 0.10f, h * 0.44f),
                    size = Size(w * 0.80f, stroke),
                    cornerRadius = r,
                )
                drawCircle(tint, radius = w * 0.17f, center = Offset(w * 0.34f, h * 0.50f))
                drawCircle(tint.copy(alpha = 0.7f), radius = w * 0.13f, center = Offset(w * 0.72f, h * 0.50f))
            }
        }
    }
}

/**
 * A friendly empty state: a mark, a headline, and a sentence that says what to do.
 *
 * The previous version was a centred title and a paragraph of grey text, which is why an empty app
 * read as unfinished rather than as ready to start.
 */
@Composable
fun EmptyState(
    mark: SectionMark,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Mark(mark = mark, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 30.dp)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}

/** A tappable row, used where a whole card opens a detail screen. */
@Composable
fun NavRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailing()
        }
    }
}

/** The screen's scrollable content, with the shared gutter already applied. */
@Composable
fun GutterColumn(
    modifier: Modifier = Modifier,
    spacedBy: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(spacedBy),
    ) { content() }
}

/** Kept so a clickable card does not need its own `Modifier.clickable` import at each call site. */
@Composable
fun ClickablePanel(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { content() }
    }
}

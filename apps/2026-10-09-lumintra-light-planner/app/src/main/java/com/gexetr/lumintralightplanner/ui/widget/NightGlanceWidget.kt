package com.gexetr.lumintralightplanner.ui.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gexetr.lumintralightplanner.MainActivity
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as ZoneTextStyle
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// The launch contract shared by the widget and the reminder.
//
// One intent shape serves both surfaces: a null spot id is the no-spot case and opens SpotEdit; a
// real id opens Brief for that spot and tonight. `internal` constants so the two surfaces cannot
// drift and MainActivity reads the same names rather than repeating string literals.
// ---------------------------------------------------------------------------------------------

internal const val EXTRA_OPEN_DESTINATION = "com.gexetr.lumintralightplanner.extra.OPEN_DESTINATION"
internal const val EXTRA_SPOT_ID = "com.gexetr.lumintralightplanner.extra.SPOT_ID"
internal const val DESTINATION_BRIEF = "brief"
internal const val DESTINATION_SPOT_EDIT = "spot_edit"

/**
 * The one intent the widget and the reminder both open: `Brief` for [spotId] and tonight, or
 * `SpotEdit` when there is no spot.
 *
 * `NEW_TASK` is required because both surfaces launch outside any task; `CLEAR_TOP` keeps a single
 * instance of the app rather than stacking a second behind the one already open.
 */
internal fun lumintraOpenIntent(context: Context, spotId: String?): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_OPEN_DESTINATION, if (spotId == null) DESTINATION_SPOT_EDIT else DESTINATION_BRIEF)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .apply { if (spotId != null) putExtra(EXTRA_SPOT_ID, spotId) }

/** The four states the widget has, and no others. Every one renders; none is a blank tile. */
enum class NightGlanceVisual {
    /** A composed brief for tonight: the four facts. */
    TODAY,

    /** A brief exists, but for a night other than tonight: never shown as tonight's. */
    OTHER_NIGHT,

    /** A spot is saved, but no brief has been composed for tonight. */
    NO_BRIEF,

    /** No spot is saved: no sky is computed for a fabricated position. */
    NO_SPOT,
}

/**
 * The widget's rendered content, resolved once per render pass from the persisted snapshot and the
 * resource table.
 *
 * Strings are resolved outside the composable so that (a) no user-facing text is built inside
 * `provideContent`, and (b) the whole decision is a pure function of (snapshot, now, resources) that
 * a unit test can assert without a composition.
 */
internal data class NightGlanceCard(
    val visual: NightGlanceVisual,
    /** The slot's wordmark. */
    val brand: String,
    /** The spot's name, or the typed state's sentence. */
    val headline: String,
    /** The dark window's bounds and the spot's zone abbreviation, or the no-darkness sentence. */
    val detail: String?,
    val moonLine: String?,
    /** The cloud figure **with** its age, or `no forecast`; never a bare number. */
    val cloudLine: String?,
    /** The one description TalkBack announces for the whole tile. */
    val a11y: String,
    /** Where a tap goes; `null` opens `SpotEdit`. */
    val spotId: String?,
    val redNightMode: Boolean,
    val darkTheme: Boolean,
) {
    val isReady: Boolean get() = visual == NightGlanceVisual.TODAY
}

/**
 * The home-screen widget: tonight's dark window, Moon and cloud outlook for one saved spot.
 *
 * **It reads and never fetches.** The draw path contains no HTTP client, no socket and no retrieval
 * of any kind — only [NightGlanceState.read] over the persisted snapshot — because MET Norway's terms
 * forbid a mobile app retrieving data while it is not in use, and a home-screen widget updates while
 * the app is not in use. The periodic update is inexact, so nothing here claims a reading is current:
 * the cloud figure is always rendered with its age, and a snapshot for another night is a typed state
 * rather than last night's window presented as tonight's.
 */
class NightGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = NightGlanceStateDefinition

    /**
     * Exactly two size classes, as the capability states them: Wide 4 × 2 (250 × 110 dp) carries the
     * four facts; Compact 2 × 2 (110 × 110 dp) carries the spot, the dark window and the Moon, and
     * omits the cloud — a bare cloud number is forbidden and the tile cannot hold a figure and its
     * age at 12 sp. The platform picks the closest declared size, so a composable only ever sees one
     * of these two.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(WIDE_SIZE, COMPACT_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = NightGlanceState.read(getAppWidgetState<Preferences>(context, id))
        // The one clock read of a render pass, taken here rather than inside the composable: the age
        // of the persisted cloud reading and the "is this tonight's snapshot" test both need a now,
        // and reading it once keeps a recomposition from inventing a second one.
        val card = resolveCard(context, snapshot, Instant.now())
        val open = actionStartActivity(lumintraOpenIntent(context, card.spotId))
        provideContent { NightGlanceTheme(card) { NightGlanceCardView(card, open, forceWide = false) } }
    }

    /**
     * The widget-picker preview: the **wide** size in the slot's own colours, so the picker shows
     * Lumintra's identity rather than a grey placeholder.
     *
     * `forceWide` is passed rather than left to the size mode because a preview composition has no
     * launcher-measured size to read. The sample carries no personal data, so the keyguard category
     * renders the same tile.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val card = resolveCard(context, previewSnapshot(context), PREVIEW_INSTANT)
        val open = actionStartActivity(lumintraOpenIntent(context, card.spotId))
        provideContent { NightGlanceTheme(card) { NightGlanceCardView(card, open, forceWide = true) } }
    }

    private companion object {
        val WIDE_SIZE = DpSize(250.dp, 110.dp)
        val COMPACT_SIZE = DpSize(110.dp, 110.dp)
    }
}

// ---------------------------------------------------------------------------------------------
// The theme bridge
// ---------------------------------------------------------------------------------------------

/**
 * Applies the slot's own palette to the widget through Glance's Material 3 colour providers.
 *
 * The three palette states are the app's own, not a second palette written for the widget:
 * [LumintraTheme] resolves light, dark and red mode from the same `Color.kt`/`NightVisionPalette.kt`
 * the screens use, and the resolved `ColorScheme` is handed to `ColorProviders`. Red mode therefore
 * reaches the widget by construction — the scheme it receives **is** `NightVisionScheme`, whose every
 * colour has blue `0x00` and green `0x00` — so no blue and no green can leak into the tile.
 *
 * `light` and `dark` receive the same scheme on purpose: the choice was already made above (red mode
 * wins over the system's night setting, as it does on screen), so the provider must not make a second,
 * possibly different, choice of its own.
 */
@Composable
private fun NightGlanceTheme(card: NightGlanceCard, content: @Composable () -> Unit) {
    LumintraTheme(redMode = card.redNightMode, darkTheme = card.darkTheme) {
        val scheme = MaterialTheme.colorScheme
        GlanceTheme(colors = ColorProviders(light = scheme, dark = scheme), content = content)
    }
}

// ---------------------------------------------------------------------------------------------
// The rendered tile
// ---------------------------------------------------------------------------------------------

/**
 * The tile, as one tap target: the mark and wordmark row, then the headline, then the detail lines.
 *
 * Wide and Compact differ by exactly one line — the cloud — because that is the fact a 2 × 2 tile
 * cannot state with its age. Text is left to reflow rather than clipped, which is what the
 * capability's 200 % font-scale requirement asks for.
 */
@Composable
private fun NightGlanceCardView(card: NightGlanceCard, open: Action, forceWide: Boolean) {
    // `||` short-circuits, so a preview never reads LocalSize: there is no launcher-measured size in
    // a preview composition.
    val wide = forceWide || LocalSize.current.width >= COMPACT_MAX_WIDTH
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(10.dp)
            .semantics { contentDescription = card.a11y }
            .clickable(open),
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            // The slot's own mark — the mono layer of the launcher icon, which is the `mark` the
            // visual-identity contract derives the icon from. Tinted from the theme, never a stock
            // library glyph; `contentDescription = null` because the whole tile carries one name.
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = card.brand,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(GlanceModifier.height(3.dp))
        Text(
            text = card.headline,
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        card.detail?.let { detail ->
            Text(
                text = detail,
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
            )
        }
        card.moonLine?.let { moon ->
            Text(
                text = moon,
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
            )
        }
        // The cloud is the one line a narrow tile drops, and it is dropped whole: a percentage without
        // the age of the reading it came from is exactly what the capability forbids.
        if (wide) {
            card.cloudLine?.let { cloud ->
                Text(
                    text = cloud,
                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 12.sp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Resolving the snapshot into a rendered card
// ---------------------------------------------------------------------------------------------

/** Below this width the launcher gave us the Compact class, and the cloud line is dropped. */
private val COMPACT_MAX_WIDTH = 200.dp

/** A fixed instant for the picker preview: the sample window is stated, never read from a clock. */
private val PREVIEW_INSTANT: Instant = Instant.parse("2026-01-15T21:04:00Z")
private const val PREVIEW_SPOT_ID = "preview"
private const val PREVIEW_ZONE = "Europe/Oslo"

/**
 * Turns a persisted snapshot into what the tile renders, or into the typed state that stands in for
 * it. Every branch here is a case the capability names; none of them is a blank tile, a placeholder
 * dash, or a value without the age of the input it came from.
 */
internal fun resolveCard(context: Context, snapshot: NightGlanceSnapshot?, nowUtc: Instant): NightGlanceCard {
    val locale: Locale = context.resources.configuration.locales[0]
    // The wordmark is the app's own brand string — the one `app_name_short` already holds and declares
    // non-translatable — rather than a second brand literal that could drift from it.
    val brand = context.getString(R.string.app_name_short)
    val darkTheme = isSystemNightMode(context)

    if (snapshot == null) {
        return stateCard(
            context = context,
            visual = NightGlanceVisual.NO_BRIEF,
            brand = brand,
            headlineRes = R.string.widget_not_ready,
            a11yRes = R.string.widget_a11y_not_ready,
            darkTheme = darkTheme,
        )
    }

    val spotName = snapshot.spotName
    val zone = snapshot.spotTzId?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() }
    if (spotName == null || zone == null) {
        return stateCard(
            context = context,
            visual = NightGlanceVisual.NO_SPOT,
            brand = brand,
            headlineRes = R.string.widget_no_spot,
            a11yRes = R.string.widget_a11y_no_spot,
            darkTheme = darkTheme,
        )
    }

    // A snapshot whose night label is not tonight's is a state, never yesterday's window shown as
    // tonight's. `SessionClock.nightOf` is the app's single definition of which night an instant
    // belongs to, so the widget cannot disagree with the brief about what "tonight" means.
    if (!NightGlanceState.describesTonight(snapshot, nowUtc)) {
        return stateCard(
            context = context,
            visual = NightGlanceVisual.OTHER_NIGHT,
            brand = brand,
            headlineRes = R.string.widget_not_ready_other_night,
            a11yRes = R.string.widget_a11y_not_ready,
            darkTheme = darkTheme,
        )
    }

    val detail = darkWindowLine(context, snapshot, zone, locale)
    if (detail == null) {
        return stateCard(
            context = context,
            visual = NightGlanceVisual.NO_BRIEF,
            brand = brand,
            headlineRes = R.string.widget_not_ready,
            a11yRes = R.string.widget_a11y_not_ready,
            darkTheme = darkTheme,
        )
    }

    val moonLine = snapshot.moonPercent?.let { percent ->
        val form = snapshot.moonForm ?: MoonForm.UNAVAILABLE
        context.getString(R.string.widget_moon_line, percent, context.getString(form.labelRes))
    }

    return NightGlanceCard(
        visual = NightGlanceVisual.TODAY,
        brand = brand,
        headline = spotName,
        detail = detail,
        moonLine = moonLine,
        cloudLine = cloudLine(context, snapshot, nowUtc),
        a11y = context.getString(R.string.widget_a11y, spotName, detail),
        spotId = snapshot.spotId,
        redNightMode = snapshot.redNightMode,
        darkTheme = darkTheme,
    )
}

/** A not-ready / no-spot card: a heading and nothing else, so no fact is ever invented for it. */
private fun stateCard(
    context: Context,
    visual: NightGlanceVisual,
    brand: String,
    headlineRes: Int,
    a11yRes: Int,
    darkTheme: Boolean,
): NightGlanceCard = NightGlanceCard(
    visual = visual,
    brand = brand,
    headline = context.getString(headlineRes),
    detail = null,
    moonLine = null,
    cloudLine = null,
    a11y = context.getString(a11yRes),
    spotId = null,
    redNightMode = false,
    darkTheme = darkTheme,
)

/**
 * The dark-window line: `23:04–02:51 CET`, or — for a night with no astronomical darkness — the
 * honest sentence for the persisted `DarkWindowKind`, abbreviated to widget size. The bounds are
 * checked first, so a polar night whose session does contain a dark interval renders that interval
 * rather than the no-interval sentence.
 *
 * A `DARK` kind with no stored bounds is an incomplete snapshot and returns `null`, which the caller
 * renders as the not-ready state; a fabricated window is never produced.
 */
private fun darkWindowLine(
    context: Context,
    snapshot: NightGlanceSnapshot,
    zone: ZoneId,
    locale: Locale,
): String? {
    val start = snapshot.darkWindowStartUtc
    val end = snapshot.darkWindowEndUtc
    if (start != null && end != null) {
        // The locale's own formatter, in the spot's zone — never the device's zone, and never two
        // strings whose difference is used as a duration.
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zone)
        val zoneLabel = zone.getDisplayName(ZoneTextStyle.SHORT, locale)
        return context.getString(
            R.string.widget_dark_window_line,
            formatter.format(start),
            formatter.format(end),
            zoneLabel,
        )
    }
    return when (snapshot.darkWindowKind) {
        DarkWindowKind.POLAR_DAY -> context.getString(R.string.widget_no_dark_polar_day)
        DarkWindowKind.POLAR_NIGHT -> context.getString(R.string.widget_no_dark_polar_night)
        DarkWindowKind.NO_ASTRO_DARK -> context.getString(R.string.widget_no_dark_no_astro)
        DarkWindowKind.NO_DARK -> context.getString(R.string.widget_no_darkness)
        DarkWindowKind.DARK, null -> null
    }
}

/**
 * The cloud line: the persisted reading **with its age**, or the typed `no forecast` string when no
 * reading exists. A percentage is never rendered without the age of the reading it came from, because
 * the widget's periodic update is inexact and an undated figure would read as current.
 */
private fun cloudLine(context: Context, snapshot: NightGlanceSnapshot, nowUtc: Instant): String? {
    val percent = snapshot.cloudPercent
    val fetchedAt = snapshot.cloudFetchedAtUtc
    if (percent == null || fetchedAt == null) return context.getString(R.string.widget_cloud_none)

    val age = Duration.between(fetchedAt, nowUtc)
    val ageLabel = when {
        age.isNegative -> context.getString(R.string.widget_cloud_age_now)
        age.toHours() >= 1L -> context.getString(R.string.widget_cloud_age_hours, age.toHours())
        else -> context.getString(
            R.string.widget_cloud_age_minutes,
            age.toMinutes().coerceAtLeast(0L),
        )
    }
    return context.getString(R.string.widget_cloud_line, percent, ageLabel)
}

/** The platform's own day/night answer, read once per render rather than a third palette switch. */
internal fun isSystemNightMode(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * The preview's snapshot: a stated sample, never a reading of the user's data and never a clock read.
 *
 * The instant is fixed and the night label is derived from it with `SessionClock.nightOf`, so the
 * sample is a well-formed tonight snapshot in the same shape the app writes.
 */
internal fun previewSnapshot(context: Context): NightGlanceSnapshot {
    val zone = ZoneId.of(PREVIEW_ZONE)
    return NightGlanceSnapshot(
        spotId = PREVIEW_SPOT_ID,
        spotName = context.getString(R.string.widget_preview_spot),
        spotTzId = PREVIEW_ZONE,
        nightDate = SessionClock.nightOf(PREVIEW_INSTANT, zone),
        darkWindowKind = DarkWindowKind.DARK,
        darkWindowStartUtc = PREVIEW_INSTANT,
        darkWindowEndUtc = PREVIEW_INSTANT.plus(Duration.ofHours(3).plusMinutes(47)),
        reminderEligible = true,
        moonPercent = 38,
        moonForm = MoonForm.UP_ALL_NIGHT,
        cloudPercent = 20,
        cloudFetchedAtUtc = PREVIEW_INSTANT.minus(Duration.ofHours(3)),
        computedOffline = false,
        computedAtUtc = PREVIEW_INSTANT,
        // Red mode is off in the preview: the capability asks for the wide size in the default theme.
        redNightMode = false,
    )
}

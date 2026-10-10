package com.gexetr.lumintralightplanner.ui.widget

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.MoonlessKind
import com.gexetr.lumintralightplanner.domain.MoonlessWindow
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SessionBrief
import com.gexetr.lumintralightplanner.domain.SessionClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * The widget's persisted state: one tonight snapshot per placed widget, written by the app when it
 * composes the brief and rendered by the widget without a network call and without any clock read
 * beyond the update that is already running.
 *
 * Every field is a primitive so the snapshot survives as Preferences and so the widget can be
 * rebuilt from disk at any moment — after a reboot, after the app is swiped away from recents, and
 * on a fresh install where nothing has been written yet (in which case [NightGlanceState.read]
 * returns `null` and the widget renders its typed not-ready state rather than a fabricated sky).
 */
data class NightGlanceSnapshot(
    /** The selected spot's id, or `null` when no spot is saved. Used only for the tap target. */
    val spotId: String?,
    /** The selected spot's name, or `null` when no spot is saved (the "Set a spot" state). */
    val spotName: String?,
    /** The spot's IANA zone id; every time the widget renders is expressed in it. */
    val spotTzId: String?,
    /** The session's local-date label, so a previous night's plan is never shown as tonight's. */
    val nightDate: LocalDate?,
    val darkWindowKind: DarkWindowKind?,
    val darkWindowStartUtc: Instant?,
    val darkWindowEndUtc: Instant?,
    /** `DarkWindowReport.reminderEligible`, carried so neither the widget nor the worker re-derives it. */
    val reminderEligible: Boolean,
    /** The Moon's illuminated fraction as a whole percentage `0..100`, or `null` when unknown. */
    val moonPercent: Int?,
    val moonForm: MoonForm?,
    /** The cached cloud fraction as a percentage, or `null` when no reading exists. */
    val cloudPercent: Int?,
    val cloudFetchedAtUtc: Instant?,
    /** The plan was composed without a network, so the reminder says so. */
    val computedOffline: Boolean,
    val computedAtUtc: Instant,
    /** The true night-vision palette is on; the widget takes its colours from the red scheme. */
    val redNightMode: Boolean,
) {
    companion object {
        /** The shape written when there is no saved spot: no sky is computed and no window is claimed. */
        fun noSpot(nowUtc: Instant, redNightMode: Boolean): NightGlanceSnapshot = NightGlanceSnapshot(
            spotId = null,
            spotName = null,
            spotTzId = null,
            nightDate = null,
            darkWindowKind = null,
            darkWindowStartUtc = null,
            darkWindowEndUtc = null,
            reminderEligible = false,
            moonPercent = null,
            moonForm = null,
            cloudPercent = null,
            cloudFetchedAtUtc = null,
            computedOffline = false,
            computedAtUtc = nowUtc,
            redNightMode = redNightMode,
        )

        /**
         * The snapshot a composed [brief] reduces to.
         *
         * The dark-window bounds are the **longest** window of the night — a two-window night renders
         * its primary window on a widget that must stay one tile. `moonPercent` is the illuminated
         * fraction rounded to a whole percentage, and the up/down form is [MoonForm.from] so both
         * surfaces agree on the words.
         */
        fun fromBrief(
            brief: SessionBrief,
            nowUtc: Instant,
            redNightMode: Boolean,
            computedOffline: Boolean,
        ): NightGlanceSnapshot {
            val window = brief.darkWindow.bestWindow
            return NightGlanceSnapshot(
                spotId = brief.spotId,
                spotName = brief.spotName,
                spotTzId = brief.tzId,
                nightDate = brief.nightDate,
                darkWindowKind = brief.darkWindow.kind,
                darkWindowStartUtc = window?.startUtc,
                darkWindowEndUtc = window?.endUtc,
                reminderEligible = brief.darkWindow.reminderEligible,
                moonPercent = (brief.moon.illuminatedFraction * 100.0).roundToInt().coerceIn(0, 100),
                moonForm = MoonForm.from(brief.moonless, brief.session),
                cloudPercent = brief.cloud?.meanCloudPercentWithin(brief.session),
                cloudFetchedAtUtc = brief.cloud?.age?.fetchedAtUtc,
                computedOffline = computedOffline,
                computedAtUtc = nowUtc,
                redNightMode = redNightMode,
            )
        }
    }
}

/**
 * The Moon's up/down form during the dark window, in the four typed words the widget and the
 * notification both use. It is derived once in [from] from the domain's [MoonlessWindow] rather than
 * re-tested at each renderer, so the two surfaces cannot word the same night differently.
 */
enum class MoonForm(@StringRes val labelRes: Int) {
    UP_ALL_NIGHT(R.string.widget_moon_up_all_night),
    DOWN_ALL_NIGHT(R.string.widget_moon_down_all_night),
    RISES(R.string.widget_moon_rises),
    SETS(R.string.widget_moon_sets),
    UNAVAILABLE(R.string.widget_moon_unknown);

    companion object {
        /**
         * Maps the domain's typed moonless outcome onto the four rendered words.
         *
         * `MOONLESS_INTERVALS` is the only ambiguous case: the Moon is either up at the window's start
         * and sets inside it, or down at the start and rises inside it. The gap's start being the
         * window's own start is exactly the second case, which is why it is read rather than guessed.
         */
        fun from(moonless: MoonlessWindow, window: NightSpan): MoonForm = when (moonless.kind) {
            MoonlessKind.MOON_UP_ALL_WINDOW, MoonlessKind.MOON_NEVER_SETS -> UP_ALL_NIGHT
            MoonlessKind.MOONLESS_ALL_NIGHT, MoonlessKind.MOON_NEVER_RISES -> DOWN_ALL_NIGHT
            MoonlessKind.MOONLESS_INTERVALS ->
                if (moonless.intervals.firstOrNull()?.startUtc == window.startUtc) RISES else SETS
            MoonlessKind.NO_DARK_WINDOW -> UNAVAILABLE
        }
    }
}

/**
 * The widget's state definition, and the one place its Preferences are read and written.
 *
 * This is Glance's own `PreferencesGlanceStateDefinition`, exposed under a slot name rather than
 * re-implemented: Glance's definition already stores the per-widget `Preferences` in a DataStore and
 * deletes it when the widget is removed, which is the behaviour the snapshot needs and nothing here
 * should restate.
 */
val NightGlanceStateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

object NightGlanceState {

    private val KEY_SPOT_ID = stringPreferencesKey("spot_id")
    private val KEY_SPOT_NAME = stringPreferencesKey("spot_name")
    private val KEY_SPOT_TZ = stringPreferencesKey("spot_tz")
    private val KEY_NIGHT_DATE = stringPreferencesKey("night_date")
    private val KEY_DARK_KIND = stringPreferencesKey("dark_window_kind")
    private val KEY_DARK_START = longPreferencesKey("dark_window_start")
    private val KEY_DARK_END = longPreferencesKey("dark_window_end")
    private val KEY_ELIGIBLE = booleanPreferencesKey("reminder_eligible")
    private val KEY_MOON_PERCENT = intPreferencesKey("moon_percent")
    private val KEY_MOON_FORM = stringPreferencesKey("moon_form")
    private val KEY_CLOUD_PERCENT = intPreferencesKey("cloud_percent")
    private val KEY_CLOUD_FETCHED = longPreferencesKey("cloud_fetched_at")
    private val KEY_OFFLINE = booleanPreferencesKey("computed_offline")
    private val KEY_COMPUTED_AT = longPreferencesKey("computed_at")
    private val KEY_RED_MODE = booleanPreferencesKey("red_night_mode")

    /**
     * Reads a snapshot, or `null` when nothing has been written yet.
     *
     * `computed_at` is the presence marker: a snapshot without it is not a snapshot, and an
     * unrecognised enum name or unparseable date degrades to the corresponding absent field rather
     * than throwing, so a downgrade cannot brick the home screen.
     */
    fun read(preferences: Preferences): NightGlanceSnapshot? {
        val computedAt = preferences[KEY_COMPUTED_AT] ?: return null
        return NightGlanceSnapshot(
            spotId = preferences[KEY_SPOT_ID],
            spotName = preferences[KEY_SPOT_NAME],
            spotTzId = preferences[KEY_SPOT_TZ],
            nightDate = preferences[KEY_NIGHT_DATE]?.let { raw ->
                runCatching { LocalDate.parse(raw) }.getOrNull()
            },
            darkWindowKind = preferences[KEY_DARK_KIND]?.let { name ->
                DarkWindowKind.entries.firstOrNull { it.name == name }
            },
            darkWindowStartUtc = preferences[KEY_DARK_START]?.let(Instant::ofEpochMilli),
            darkWindowEndUtc = preferences[KEY_DARK_END]?.let(Instant::ofEpochMilli),
            reminderEligible = preferences[KEY_ELIGIBLE] ?: false,
            moonPercent = preferences[KEY_MOON_PERCENT],
            moonForm = preferences[KEY_MOON_FORM]?.let { name ->
                MoonForm.entries.firstOrNull { it.name == name }
            },
            cloudPercent = preferences[KEY_CLOUD_PERCENT],
            cloudFetchedAtUtc = preferences[KEY_CLOUD_FETCHED]?.let(Instant::ofEpochMilli),
            computedOffline = preferences[KEY_OFFLINE] ?: false,
            computedAtUtc = Instant.ofEpochMilli(computedAt),
            redNightMode = preferences[KEY_RED_MODE] ?: false,
        )
    }

    /**
     * True when the snapshot describes the night the given instant belongs to, in the spot's own zone.
     *
     * The comparison is [SessionClock.nightOf]'s — the app's single definition of which night an
     * instant belongs to — so the widget cannot disagree with the brief about what "tonight" means.
     * A snapshot for any other night is the state the requirement forbids showing as tonight's.
     */
    fun describesTonight(snapshot: NightGlanceSnapshot, nowUtc: Instant): Boolean {
        val label = snapshot.nightDate ?: return false
        val zone = snapshot.spotTzId?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() } ?: return false
        return SessionClock.nightOf(nowUtc, zone) == label
    }

    /**
     * Writes [snapshot] to every placed widget and pushes a re-render, returning how many widgets were
     * updated. Zero means no widget is placed, which is not an error — the app keeps working.
     *
     * Both halves are the requirement's: writing without the push would leave the widget showing the
     * previous reading until the platform's inexact periodic update arrived.
     */
    suspend fun write(context: Context, snapshot: NightGlanceSnapshot): Int {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(NightGlanceWidget::class.java)
        if (glanceIds.isEmpty()) return 0
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, NightGlanceStateDefinition, glanceId) {
                mutablePreferencesOf().apply { applySnapshot(snapshot) }
            }
        }
        NightGlanceWidget().updateAll(context)
        return glanceIds.size
    }

    /**
     * Updates only the red-night-mode flag on every placed widget.
     *
     * The palette is a display setting independent of the night's facts, so toggling it must not need
     * a recomposed brief — but it must still reach the persisted state, because the snapshot is what
     * the widget's render reads.
     */
    suspend fun setRedNightMode(context: Context, enabled: Boolean): Int {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(NightGlanceWidget::class.java)
        if (glanceIds.isEmpty()) return 0
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, NightGlanceStateDefinition, glanceId) { current ->
                val snapshot = read(current)?.copy(redNightMode = enabled) ?: return@updateAppWidgetState current
                mutablePreferencesOf().apply { applySnapshot(snapshot) }
            }
        }
        NightGlanceWidget().updateAll(context)
        return glanceIds.size
    }

    /**
     * Writes the "no spot saved" shape, so a deleted spot immediately stops the widget claiming a
     * window it can no longer compute. Called with the instant of the delete.
     */
    suspend fun writeNoSpot(context: Context, nowUtc: Instant, redNightMode: Boolean): Int =
        write(context, NightGlanceSnapshot.noSpot(nowUtc, redNightMode))

    private fun MutablePreferences.applySnapshot(snapshot: NightGlanceSnapshot) {
        setOrRemove(KEY_SPOT_ID, snapshot.spotId)
        setOrRemove(KEY_SPOT_NAME, snapshot.spotName)
        setOrRemove(KEY_SPOT_TZ, snapshot.spotTzId)
        setOrRemove(KEY_NIGHT_DATE, snapshot.nightDate?.toString())
        setOrRemove(KEY_DARK_KIND, snapshot.darkWindowKind?.name)
        setOrRemove(KEY_DARK_START, snapshot.darkWindowStartUtc?.toEpochMilli())
        setOrRemove(KEY_DARK_END, snapshot.darkWindowEndUtc?.toEpochMilli())
        this[KEY_ELIGIBLE] = snapshot.reminderEligible
        setOrRemove(KEY_MOON_PERCENT, snapshot.moonPercent)
        setOrRemove(KEY_MOON_FORM, snapshot.moonForm?.name)
        setOrRemove(KEY_CLOUD_PERCENT, snapshot.cloudPercent)
        setOrRemove(KEY_CLOUD_FETCHED, snapshot.cloudFetchedAtUtc?.toEpochMilli())
        this[KEY_OFFLINE] = snapshot.computedOffline
        this[KEY_COMPUTED_AT] = snapshot.computedAtUtc.toEpochMilli()
        this[KEY_RED_MODE] = snapshot.redNightMode
    }

    private fun <T> MutablePreferences.setOrRemove(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }
}

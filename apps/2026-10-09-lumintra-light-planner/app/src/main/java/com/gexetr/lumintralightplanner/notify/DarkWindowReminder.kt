package com.gexetr.lumintralightplanner.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.gexetr.lumintralightplanner.MainActivity
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.ui.widget.DESTINATION_BRIEF
import com.gexetr.lumintralightplanner.ui.widget.EXTRA_OPEN_DESTINATION
import com.gexetr.lumintralightplanner.ui.widget.EXTRA_SPOT_ID
import com.gexetr.lumintralightplanner.ui.widget.MoonForm
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** What a re-derivation did, so the caller can state it rather than guess. */
sealed interface ReminderReconcileResult {

    /** Exactly one work item is pending, for [fireAtUtc] on [nightDate] at [spotId]. */
    data class Armed(
        val fireAtUtc: Instant,
        val nightDate: LocalDate,
        val spotId: String,
    ) : ReminderReconcileResult

    /** The preference is off, or no plan was supplied: nothing is scheduled and nothing is posted. */
    data object Disabled : ReminderReconcileResult

    /** The night offers no dark window the domain calls eligible, so nothing is scheduled. */
    data object NotEligible : ReminderReconcileResult

    /** Every candidate window start has already passed; the next night is armed on the next open. */
    data object AlreadyPast : ReminderReconcileResult
}

/**
 * The opt-in reminder: **one** local notification at the start of tonight's dark window.
 *
 * Design in one paragraph. It is off until the user turns it on; it is armed by an explicit call to
 * [reconcile] and never by launch, onboarding or a first-run flow; it is exactly one
 * `OneTimeWorkRequest` under [UNIQUE_WORK_NAME] with `ExistingWorkPolicy.REPLACE`, so a second arm
 * replaces the first rather than stacking. It is **inexact** — `WorkManager`'s own one-time work with
 * an initial delay, and no `AlarmManager` call of any kind — because the dark window is derived from
 * the sky and precise to the minute at best, so a delivery inside the window is still a true reminder
 * while an exact alarm would be a restricted permission taken for a precision the product lacks. The
 * work's input data is a complete record of the facts to post, so the worker needs no repository and
 * no network.
 *
 * The preference itself is not stored here. [reconcile] takes `enabled` as its first argument, so the
 * one stored value stays the one the `Brief` and `Settings` surfaces already share and this module
 * cannot become a second answer to "is the reminder on".
 */
object DarkWindowReminder {

    /** The single unique work name. One name is what makes "at most one reminder" structural. */
    const val UNIQUE_WORK_NAME: String = "lumintra.dark_window_reminder"

    /** A fixed channel id: a re-post replaces rather than stacking. */
    const val CHANNEL_ID: String = "lumintra.night_reminder"

    /** A fixed notification id, for the same reason. */
    const val NOTIFICATION_ID: Int = 4201

    /** A tag on the work item, so what is pending is readable without decoding the plan. */
    const val WORK_TAG: String = "lumintra.reminder"

    // ---------------------------------------------------------------------------------------------
    // Re-derivation
    // ---------------------------------------------------------------------------------------------

    /**
     * Derives the reminder from scratch: cancel whatever is armed, then arm the first candidate night
     * whose window start is still ahead.
     *
     * This is what every app open, every spot change and every recomposed plan calls, because a
     * reminder must never fire for a plan the user has since changed. [candidates] is tonight's plan
     * first and the **next** night's after it: a past instant therefore arms the following night
     * rather than nothing, and a night with no dark window cancels anything still armed.
     *
     * Cancelling first and enqueueing second is deliberate. The unique name already guarantees at most
     * one work item; the extra cancel is what makes "turning it off leaves nothing scheduled" true even
     * when the caller passes `enabled = false` and no plan at all.
     */
    fun reconcile(
        context: Context,
        enabled: Boolean,
        candidates: List<ReminderPlan>,
        nowUtc: Instant,
    ): ReminderReconcileResult {
        if (!enabled || candidates.isEmpty()) {
            cancel(context)
            return ReminderReconcileResult.Disabled
        }

        return when (val choice = ReminderScheduler.chooseFirstFuture(candidates, nowUtc)) {
            is ReminderPlanChoice.Arm -> {
                val plan = candidates[choice.planIndex]
                arm(context, plan, choice.fireAtUtc, choice.delay)
                ReminderReconcileResult.Armed(choice.fireAtUtc, plan.nightDate, plan.spotId)
            }

            ReminderPlanChoice.AlreadyPast -> {
                // Nothing for tonight is in the future. Drop the armed record rather than leave a
                // stale instant behind, and let the next open arm the following night.
                cancelWorkOnly(context)
                ReminderStore.clearArmed(context)
                ReminderReconcileResult.AlreadyPast
            }

            ReminderPlanChoice.NotEligible -> {
                // A night with no dark window does not offer a reminder, and one armed before the plan
                // changed to such a night is cancelled rather than left to fire.
                cancel(context)
                ReminderReconcileResult.NotEligible
            }
        }
    }

    /**
     * Cancels the pending work **and** any notification already posted, leaving nothing scheduled.
     * This is what turning the reminder off calls.
     */
    fun cancel(context: Context) {
        cancelWorkOnly(context)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        ReminderStore.clear(context)
    }

    private fun cancelWorkOnly(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun arm(context: Context, plan: ReminderPlan, fireAtUtc: Instant, delay: java.time.Duration) {
        // The channel exists before the first post, and creating it here means it is already visible in
        // the system settings once the user opts in.
        ensureChannel(context)
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<DarkWindowReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(plan.toWorkData())
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        ReminderStore.recordArmed(context, plan, fireAtUtc)
    }

    // ---------------------------------------------------------------------------------------------
    // Observability — what this module armed, as its own record
    // ---------------------------------------------------------------------------------------------

    /**
     * The instant this module last armed, or `null` when nothing is recorded.
     *
     * This is **our own record of what we asked for**, not a probe of `WorkManager`'s queue; the honest
     * limit is that it cannot observe a work item removed by something else. It exists so the `Brief`
     * and `Settings` surfaces can state the reminder's state in words rather than by colour.
     */
    fun armedInstant(context: Context): Instant? = ReminderStore.armedInstant(context)

    /** The night the armed reminder belongs to, or `null`. */
    fun armedNightDate(context: Context): LocalDate? = ReminderStore.armedNightDate(context)

    /**
     * True when the armed record is spent — a delivery was displaced past the window's end, arrived
     * with notifications off, or was posted — so the app re-derives on its next open rather than
     * trusting the record.
     */
    fun needsRederivation(context: Context): Boolean = ReminderStore.needsRederivation(context)

    // ---------------------------------------------------------------------------------------------
    // The channel and the post
    // ---------------------------------------------------------------------------------------------

    /**
     * Creates the reminder's one channel, idempotently.
     *
     * `IMPORTANCE_DEFAULT` is deliberate: noticeable without a full-screen intent, without a custom
     * sound and without a vibration pattern of the app's own. Called on API 26–32 as well, where the
     * permission does not exist as a runtime grant — the reminder works there with no prompt.
     */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminder_channel_name))
            .setDescription(context.getString(R.string.reminder_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Posts the reminder for [plan].
     *
     * Every string is a resource and every time is the locale's own format in the **spot's** zone. The
     * title is a statement of the window's start, never an instruction and never a promise about the
     * sky; the cloud figure is labelled a forecast and carries the time it was retrieved. The one
     * action opens tonight's `Brief`, and there is no second action to dismiss or to do anything else.
     */
    fun post(context: Context, plan: ReminderPlan) {
        val window = plan.window ?: return
        val zone = runCatching { ZoneId.of(plan.tzId) }.getOrNull() ?: return
        val locale: Locale = context.resources.configuration.locales[0]
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zone)
        val zoneLabel = zone.getDisplayName(TextStyle.SHORT, locale)

        val windowLine = context.getString(
            R.string.reminder_body_window,
            time.format(window.startUtc),
            time.format(window.endUtc),
            zoneLabel,
        )
        val lines = buildList {
            add(context.getString(R.string.reminder_body_spot, plan.spotName))
            add(windowLine)
            plan.moonPercent?.let { percent ->
                add(
                    context.getString(
                        R.string.reminder_body_moon,
                        percent,
                        context.getString(plan.moonForm.labelRes),
                    ),
                )
            }
            val cloudPercent = plan.cloudPercent
            val cloudFetchedAt = plan.cloudFetchedAtUtc
            if (cloudPercent != null && cloudFetchedAt != null) {
                add(context.getString(R.string.reminder_body_cloud, cloudPercent, time.format(cloudFetchedAt)))
            } else {
                add(context.getString(R.string.reminder_body_no_cloud))
            }
            if (plan.computedOffline) add(context.getString(R.string.reminder_body_offline))
        }

        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ui_bell)
            .setContentTitle(context.getString(R.string.reminder_title, time.format(window.startUtc)))
            .setContentText(windowLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(openBrief(context, plan.spotId))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /**
     * The one tap target: `Brief` for the spot and tonight.
     *
     * `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` — immutable because nothing rewrites the extras after the
     * fact, update-current so a re-arm for another spot replaces the previous target instead of leaving
     * a stale one behind.
     */
    private fun openBrief(context: Context, spotId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_DESTINATION, DESTINATION_BRIEF)
                .putExtra(EXTRA_SPOT_ID, spotId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    // ---------------------------------------------------------------------------------------------
    // The plan, as the work's own persisted input
    // ---------------------------------------------------------------------------------------------

    private const val KEY_SPOT_ID = "spot_id"
    private const val KEY_SPOT_NAME = "spot_name"
    private const val KEY_TZ = "tz_id"
    private const val KEY_NIGHT_DATE = "night_date"
    private const val KEY_KIND = "dark_window_kind"
    private const val KEY_START = "dark_window_start"
    private const val KEY_END = "dark_window_end"
    private const val KEY_ELIGIBLE = "reminder_eligible"
    private const val KEY_MOON_PERCENT = "moon_percent"
    private const val KEY_MOON_FORM = "moon_form"
    private const val KEY_CLOUD_PERCENT = "cloud_percent"
    private const val KEY_CLOUD_FETCHED = "cloud_fetched_at"
    private const val KEY_OFFLINE = "computed_offline"

    /**
     * The plan as `Data`, writing only the keys that have a value.
     *
     * `WorkManager` persists a queued request's input across a reboot, which is what makes the worker's
     * "the persisted dark window" a real thing rather than a hope: at fire time the record below is the
     * same record that was armed, with no repository and no network in between. An absent optional key
     * means "unknown", never a zero that would be rendered as a real reading.
     */
    private fun ReminderPlan.toWorkData(): Data {
        val builder = Data.Builder()
            .putString(KEY_SPOT_ID, spotId)
            .putString(KEY_SPOT_NAME, spotName)
            .putString(KEY_TZ, tzId)
            .putString(KEY_NIGHT_DATE, nightDate.toString())
            .putString(KEY_KIND, kind.name)
            .putBoolean(KEY_ELIGIBLE, reminderEligible)
            .putString(KEY_MOON_FORM, moonForm.name)
            .putBoolean(KEY_OFFLINE, computedOffline)
        window?.let { span ->
            builder.putLong(KEY_START, span.startUtc.toEpochMilli())
            builder.putLong(KEY_END, span.endUtc.toEpochMilli())
        }
        moonPercent?.let { builder.putInt(KEY_MOON_PERCENT, it) }
        cloudPercent?.let { builder.putInt(KEY_CLOUD_PERCENT, it) }
        cloudFetchedAtUtc?.let { builder.putLong(KEY_CLOUD_FETCHED, it.toEpochMilli()) }
        return builder.build()
    }

    /**
     * The inverse, degrading to `null` rather than throwing.
     *
     * A record missing a required field, or naming an enum constant this build no longer has, is not a
     * reminder: the worker completes without posting rather than crashing on the user's home screen.
     */
    internal fun planFromWorkData(data: Data): ReminderPlan? {
        val spotId = data.getString(KEY_SPOT_ID) ?: return null
        val spotName = data.getString(KEY_SPOT_NAME) ?: return null
        val tzId = data.getString(KEY_TZ) ?: return null
        val nightDate = data.getString(KEY_NIGHT_DATE)
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
            ?: return null
        val kind = data.getString(KEY_KIND)
            ?.let { name -> DarkWindowKind.entries.firstOrNull { it.name == name } }
            ?: return null
        val window = if (data.hasKeyWithValueOfType<Long>(KEY_START) && data.hasKeyWithValueOfType<Long>(KEY_END)) {
            // The domain's own half-open span, so the worker's membership test is the same one the rest
            // of the app uses rather than a second comparison of two longs.
            NightSpan(
                Instant.ofEpochMilli(data.getLong(KEY_START, 0L)),
                Instant.ofEpochMilli(data.getLong(KEY_END, 0L)),
                spotId,
            )
        } else {
            null
        }
        return ReminderPlan(
            spotId = spotId,
            spotName = spotName,
            tzId = tzId,
            nightDate = nightDate,
            kind = kind,
            window = window,
            reminderEligible = data.getBoolean(KEY_ELIGIBLE, false),
            moonPercent = if (data.hasKeyWithValueOfType<Int>(KEY_MOON_PERCENT)) {
                data.getInt(KEY_MOON_PERCENT, 0)
            } else {
                null
            },
            moonForm = data.getString(KEY_MOON_FORM)
                ?.let { name -> MoonForm.entries.firstOrNull { it.name == name } }
                ?: MoonForm.UNAVAILABLE,
            cloudPercent = if (data.hasKeyWithValueOfType<Int>(KEY_CLOUD_PERCENT)) {
                data.getInt(KEY_CLOUD_PERCENT, 0)
            } else {
                null
            },
            cloudFetchedAtUtc = if (data.hasKeyWithValueOfType<Long>(KEY_CLOUD_FETCHED)) {
                Instant.ofEpochMilli(data.getLong(KEY_CLOUD_FETCHED, 0L))
            } else {
                null
            },
            computedOffline = data.getBoolean(KEY_OFFLINE, false),
        )
    }
}

/**
 * The worker: posts the reminder if — and only if — the instant it runs is inside the persisted dark
 * window.
 *
 * Every path returns `Result.success()`. Nothing here retries: a reminder that could not be posted is
 * a reminder for a window that has passed, or a permission the user withheld, and neither is fixed by
 * trying again. When it does not post it marks the record as spent, so the app re-derives on its next
 * open and a delivery displaced by a reboot or a clock change never announces a window that is over.
 */
class DarkWindowReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val plan = DarkWindowReminder.planFromWorkData(inputData)
        if (plan == null) {
            ReminderStore.clear(applicationContext)
            return Result.success()
        }

        // The permission may have been revoked after the reminder was armed. Completing without error
        // and without retry is the required behaviour: the app states that notifications are off for
        // Lumintra and offers the control that opens the setting, and it does not claim the reminder
        // is armed.
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            ReminderStore.markSpent(applicationContext)
            return Result.success()
        }

        // The domain's own eligibility, not a second reading of the rule: a polar day or a non-dark
        // outcome never posts, and a polar night posts only because it carries a real window.
        if (!plan.reminderEligible) {
            ReminderStore.markSpent(applicationContext)
            return Result.success()
        }
        val window = plan.window ?: run {
            ReminderStore.markSpent(applicationContext)
            return Result.success()
        }

        val now = Instant.now()

        // The delivery may have been displaced past the window by a reboot, a doze window or a clock
        // change. `NightSpan.contains` is half-open `[start, end)`, so a delivery exactly at the end is
        // outside the window and posts nothing.
        if (now !in window) {
            ReminderStore.markSpent(applicationContext)
            return Result.success()
        }

        // Belt and braces against a clock move: the plan must still describe the night this instant
        // belongs to, by the app's own definition of which night an instant is in.
        val zone = runCatching { ZoneId.of(plan.tzId) }.getOrNull()
        if (zone == null || SessionClock.nightOf(now, zone) != plan.nightDate) {
            ReminderStore.markSpent(applicationContext)
            return Result.success()
        }

        DarkWindowReminder.post(applicationContext, plan)
        // Delivered, so the armed instant is spent: the previous night's reminder is consumed and the
        // next night's is armed on the next open, never by a repeating or periodic work.
        ReminderStore.markSpent(applicationContext)
        return Result.success()
    }
}

/**
 * The reminder's own small persisted record: what we armed, and whether that record is spent.
 *
 * It is **not** the reminder's preference — that lives in the app's one settings store — and it is not
 * a probe of `WorkManager`'s queue. It is the module's own note, and its honest limit is that it cannot
 * observe a work item removed by anything other than this module; what it is for is letting the `Brief`
 * and `Settings` surfaces state the reminder's state in words instead of by colour.
 */
private object ReminderStore {

    private const val FILE = "lumintra_reminder"
    private const val KEY_ARMED_AT = "armed_at"
    private const val KEY_ARMED_NIGHT = "armed_night"
    private const val KEY_ARMED_SPOT = "armed_spot"
    private const val KEY_SPENT = "spent"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun recordArmed(context: Context, plan: ReminderPlan, fireAtUtc: Instant) {
        prefs(context).edit()
            .putLong(KEY_ARMED_AT, fireAtUtc.toEpochMilli())
            .putString(KEY_ARMED_NIGHT, plan.nightDate.toString())
            .putString(KEY_ARMED_SPOT, plan.spotId)
            .putBoolean(KEY_SPENT, false)
            .apply()
    }

    /** The armed instant is spent or superseded; the night it belonged to stays on the record. */
    fun clearArmed(context: Context) {
        prefs(context).edit()
            .remove(KEY_ARMED_AT)
            .remove(KEY_ARMED_SPOT)
            .apply()
    }

    /** The delivery happened, or could not happen: either way the record needs a re-derivation. */
    fun markSpent(context: Context) {
        prefs(context).edit()
            .remove(KEY_ARMED_AT)
            .putBoolean(KEY_SPENT, true)
            .apply()
    }

    fun armedInstant(context: Context): Instant? = prefs(context).let { store ->
        if (store.contains(KEY_ARMED_AT)) Instant.ofEpochMilli(store.getLong(KEY_ARMED_AT, 0L)) else null
    }

    fun armedNightDate(context: Context): LocalDate? = prefs(context)
        .getString(KEY_ARMED_NIGHT, null)
        ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

    fun needsRederivation(context: Context): Boolean = prefs(context).getBoolean(KEY_SPENT, false)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

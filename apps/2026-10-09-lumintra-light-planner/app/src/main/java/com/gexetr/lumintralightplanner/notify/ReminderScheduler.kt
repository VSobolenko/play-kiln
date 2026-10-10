package com.gexetr.lumintralightplanner.notify

import com.gexetr.lumintralightplanner.domain.DarkWindowKind
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SessionBrief
import com.gexetr.lumintralightplanner.ui.widget.MoonForm
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Everything one armed reminder needs, as a plain value.
 *
 * It is deliberately **all** of the reminder's facts rather than the arithmetic's inputs alone: the
 * worker runs when the app is not running, so the work's own input data must be a complete record of
 * what to post, and a plan that omitted the Moon or the cloud would force a second source of truth at
 * fire time. The arithmetic below reads only the scheduling half of it.
 *
 * Every time is an absolute UTC instant and every number is already rounded, so no formatting and no
 * clock decision happens here. This type carries no Android and no `Context`, which is what makes the
 * whole module testable on the JVM.
 */
data class ReminderPlan(
    /** The spot the reminder belongs to; opens `Brief` for it. */
    val spotId: String,
    val spotName: String,
    /** The spot's IANA zone: the notification's times are shown in it, never in the device's. */
    val tzId: String,
    /** The night this plan describes, so a plan for another night is never posted as tonight's. */
    val nightDate: LocalDate,
    val kind: DarkWindowKind,
    /** The night's longest dark window, or `null` when the night has none. */
    val window: NightSpan?,
    /** `DarkWindowReport.reminderEligible`, carried rather than re-derived. */
    val reminderEligible: Boolean,
    /** The Moon's illuminated fraction as a whole percentage `0..100`, or `null` when unknown. */
    val moonPercent: Int?,
    val moonForm: MoonForm,
    /** The cached cloud reading's value, or `null` when no reading exists. */
    val cloudPercent: Int?,
    /** When that reading was retrieved, so the notification can label it a forecast with its time. */
    val cloudFetchedAtUtc: Instant?,
    /** The plan was composed without a network, so the notification says so rather than implying a fresh forecast. */
    val computedOffline: Boolean,
)

/**
 * What to do about one night's reminder. A sealed result rather than a nullable instant, because
 * "the window has already started" and "this night has no window at all" are different answers with
 * different consequences — the first arms the next night, the second arms nothing.
 */
sealed interface ReminderDecision {

    /** Enqueue one work at [fireAtUtc], which is [delay] from now. */
    data class Arm(val fireAtUtc: Instant, val delay: Duration) : ReminderDecision

    /**
     * The window's start is not in the future, so nothing may be enqueued for it. The caller arms the
     * **next** night's window instead; a notification for a window that has started (or ended) is
     * wrong, and a notification for a window that ended is worse.
     */
    data object AlreadyPast : ReminderDecision

    /** The night offers no dark window to remind about, or offers none the domain calls eligible. */
    data object NotEligible : ReminderDecision
}

/**
 * Which candidate night to arm, and why not when none can be.
 *
 * [Arm.planIndex] is carried rather than the plan itself so the caller looks up the plan it already
 * holds instead of matching on the instant — a lookup that silently fails the moment a non-zero lead
 * time makes the fire instant differ from the window's start.
 */
sealed interface ReminderPlanChoice {

    /** Arm `candidates[planIndex]` at [fireAtUtc], which is [delay] from now. */
    data class Arm(val planIndex: Int, val fireAtUtc: Instant, val delay: Duration) : ReminderPlanChoice

    /** An eligible night exists but its window start has passed: the next night is the one to arm. */
    data object AlreadyPast : ReminderPlanChoice

    /** No candidate night offers a dark window the domain calls eligible. */
    data object NotEligible : ReminderPlanChoice
}

/**
 * The reminder's arithmetic, with no `Context`, no `WorkManager` and no Android type: given a night's
 * plan and a now, it decides the instant to fire and the delay to reach it.
 *
 * The reminder is **inexact by design**. Nothing here asks for an exact alarm, and nothing here needs
 * one: the dark window is derived from the sky and precise to the minute at best, so a delivery inside
 * it is still a true reminder while an exact alarm would be a restricted permission taken for a
 * precision the product does not have.
 */
object ReminderScheduler {

    /**
     * How far before the dark window's start the reminder is asked for. Zero is the specified
     * behaviour — the reminder is the statement that the window is beginning — and it is a parameter
     * only so the arithmetic is stated once and can be tested with a non-zero lead.
     */
    val LEAD_TIME: Duration = Duration.ZERO

    /**
     * The instant the reminder fires for a window starting at [windowStartUtc].
     *
     * The window start is an absolute UTC instant and is never rebuilt from a formatted string, so a
     * device time-zone change cannot move it: the spot's zone is a property of the spot.
     */
    fun reminderInstant(windowStartUtc: Instant, leadTime: Duration = LEAD_TIME): Instant =
        windowStartUtc.minus(leadTime)

    /**
     * The delay to hand `WorkRequest.Builder.setInitialDelay`.
     *
     * A negative or zero difference becomes [Duration.ZERO] rather than a negative delay, because
     * `WorkManager` rejects a negative one and a reminder that is due now is not an error.
     */
    fun initialDelay(nowUtc: Instant, fireAtUtc: Instant): Duration {
        val delay = Duration.between(nowUtc, fireAtUtc)
        return if (delay.isNegative) Duration.ZERO else delay
    }

    /**
     * The decision for one night: arm it, skip it as past, or report that there is nothing to arm.
     *
     * Eligibility is [ReminderPlan.reminderEligible]'s — the domain's own answer, `DarkWindowKind.DARK`
     * or a polar night that does contain a dark interval — and never re-derived here, so a polar day, a
     * `NO_ASTRO_DARK` or a `NO_DARK` night cannot be offered a reminder by a second reading of the rule.
     */
    fun decide(
        plan: ReminderPlan,
        nowUtc: Instant,
        leadTime: Duration = LEAD_TIME,
    ): ReminderDecision {
        if (!plan.reminderEligible) return ReminderDecision.NotEligible
        val window = plan.window ?: return ReminderDecision.NotEligible
        val fireAt = reminderInstant(window.startUtc, leadTime)
        // Strictly in the future: an instant that has already arrived is not something to schedule.
        if (!fireAt.isAfter(nowUtc)) return ReminderDecision.AlreadyPast
        return ReminderDecision.Arm(fireAt, initialDelay(nowUtc, fireAt))
    }

    /**
     * The first candidate night that can be armed, and why when none can.
     *
     * The candidates are tonight first and the following night after it. A past instant therefore
     * arms the **next** night rather than nothing, which is what the capability requires, and the next
     * night's window is taken from a brief the app composed for that night — this module never shifts
     * tonight's window by a day, because tomorrow's darkness is not tonight's plus 24 hours.
     *
     * Both answers are computed by [decide] over the same list, so the eligibility rule and the
     * past-instant rule exist once and this function cannot disagree with a caller that asks about a
     * single night.
     */
    fun chooseFirstFuture(
        candidates: List<ReminderPlan>,
        nowUtc: Instant,
        leadTime: Duration = LEAD_TIME,
    ): ReminderPlanChoice {
        var sawEligibleNight = false
        candidates.forEachIndexed { index, plan ->
            when (val decision = decide(plan, nowUtc, leadTime)) {
                is ReminderDecision.Arm ->
                    return ReminderPlanChoice.Arm(index, decision.fireAtUtc, decision.delay)

                ReminderDecision.AlreadyPast -> sawEligibleNight = true
                ReminderDecision.NotEligible -> Unit
            }
        }
        return if (sawEligibleNight) ReminderPlanChoice.AlreadyPast else ReminderPlanChoice.NotEligible
    }

    /**
     * The plan a composed [brief] reduces to.
     *
     * This is the **one** place the reminder's wording of a night is derived, and it takes its Moon
     * form from the same [MoonForm.from] the widget's snapshot uses, so the two surfaces cannot word
     * the same night differently. The cloud value is the same `meanCloudPercentWithin` the widget
     * stores, so a reminder and a tile describing one night state one cloud figure.
     */
    fun planOf(brief: SessionBrief, computedOffline: Boolean): ReminderPlan {
        val window = brief.darkWindow.bestWindow
        return ReminderPlan(
            spotId = brief.spotId,
            spotName = brief.spotName,
            tzId = brief.tzId,
            nightDate = brief.nightDate,
            kind = brief.darkWindow.kind,
            window = window,
            reminderEligible = brief.darkWindow.reminderEligible,
            moonPercent = (brief.moon.illuminatedFraction * 100.0).roundToInt().coerceIn(0, 100),
            moonForm = MoonForm.from(brief.moonless, brief.session),
            cloudPercent = brief.cloud?.meanCloudPercentWithin(brief.session),
            cloudFetchedAtUtc = brief.cloud?.age?.fetchedAtUtc,
            computedOffline = computedOffline,
        )
    }
}

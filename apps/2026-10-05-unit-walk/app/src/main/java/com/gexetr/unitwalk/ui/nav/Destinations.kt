package com.gexetr.unitwalk.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The slot's navigation keys, in one file, because three things depend on the same seven names:
 * [com.gexetr.unitwalk.ui.UnitWalkApp] routes between them, each screen is written against the
 * callbacks they carry, and Navigation 3 persists them (`@Serializable`) across a process kill.
 * A key declared here is the contract; a screen never invents a route.
 *
 * Every screen that a walk id can reach carries it as a constructor argument rather than reading a
 * shared holder, so a destination is a function of its argument: `rememberNavBackStack` persists these
 * keys, and a stack restored after a process kill therefore names the same walks it named before.
 */
@Serializable
data object UnitsDestination : NavKey

/**
 * One walk, by id.
 *
 * The id always names a walk that is already on disk: the portfolio persists a new walk *before* it
 * tells the shell to open it (`ui/units/UnitsScreen.kt:92-98`, `UnitsViewModel.startWalkForUnit`), so
 * there is no "walk that does not exist yet" state for this key to carry and no sentinel value.
 */
@Serializable
data class WalkDestination(val walkId: String) : NavKey

/** Move-in versus move-out for [walkId]; also the state in which a walk is finished. */
@Serializable
data class CompareDestination(val walkId: String) : NavKey

/** The finished report for [walkId], signed inside the app. */
@Serializable
data class ReportDestination(val walkId: String) : NavKey

/**
 * The one-time unlock: unlimited units, and PDF reports without the watermark.
 *
 * It does not gate export — a free report is always writable, shareable and savable, and carries the
 * stamp described by `pdf/ReportWatermark.kt`. One purchase, no renewal.
 */
@Serializable
data object UnlockDestination : NavKey

@Serializable
data object SettingsDestination : NavKey

@Serializable
data object AboutDestination : NavKey

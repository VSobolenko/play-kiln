package com.gexetr.laneset.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Laneset's shape scale.
 *
 * The deck has exactly two geometries and this file is built from both. A **lane rope** is a line
 * with a rounded terminal: every chip, every pill, every progress track is fully round-ended or it
 * is a plain rule, and nothing in between. A **panel** is a flat slab — a whiteboard, a clipboard,
 * a tile — with a corner small enough that it reads as a surface rather than as a bubble. The M3
 * scale below therefore grows in one direction, from the chip (4dp) to the hero (24dp), and the two
 * shapes it cannot express are the two named constants at the bottom.
 *
 * The values are deliberately not the Material baseline's (4/8/12/16/28): a 28dp extra-large corner
 * on a full-width panel bulges into a lozenge on a 360dp phone. This slot's largest all-corner
 * radius is 24dp, and [DeckSlabShape] — a panel anchored on its bottom edge — is the only shape in
 * the app that is asymmetric.
 *
 * These are constants rather than a composition local, unlike the colours and the numeric type:
 * a shape does not change between light and dark or between one preference and another, so a local
 * would be a provider with one value and a lookup that can never return anything else.
 */

/** The M3 slot scale, as the slot's own values. */
internal val LanesetShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * A pill: a section chip, a status pill, a pace track, the rep bar on an interval row.
 *
 * `percent = 50` rather than a large dp radius, so the shape stays a pill when its text grows in a
 * longer locale and the row gets taller. A fixed 20dp corner on a row that has grown to 56dp reads
 * as a rounded rectangle, which is a different component.
 */
val LaneRopeShape: Shape = RoundedCornerShape(percent = 50)

/**
 * A panel anchored to the deck: the target-time hero, the set-list sheet.
 *
 * Round on its top corners and square where it meets the surface below it, so a sheet reads as a
 * thing the swimmer is looking down at rather than as a floating card. No M3 slot is asymmetric,
 * which is why this one is named here rather than given a slot it would have to fight for.
 */
val DeckSlabShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

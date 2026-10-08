package com.gexetr.unitwalk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * UnitWalk's shapes. A shape is a decision the app makes repeatedly, so it lives here rather than
 * being spelled out per call site: a walk report is a document, and this slot's geometry says so —
 * panels are cut with one shallow corner (the sheet a form is printed on), the strip and every status
 * pill are fully round (the label you stick on a door), and nothing in the app is a square rectangle
 * with a 4dp radius, which is what a slot looks like when it did not decide.
 *
 * The asymmetric corners use `topStart` / `topEnd` / `bottomStart` / `bottomEnd` rather than
 * left/right, so the cut mirrors under a right-to-left locale instead of moving to the wrong corner.
 */

/** A tile, a thumbnail, a small inset: the tightest radius the app draws with. */
internal val TileCorner = RoundedCornerShape(12.dp)

/** The default panel: a sheet, with its lower-trailing corner cut back so a stack of them reads. */
internal val SheetCorner = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,
    bottomEnd = 6.dp,
)

/** A section strip and every status pill: fully round, so a label is never mistaken for a panel. */
internal val PillCorner = RoundedCornerShape(percent = 50)

/** A hero field: the widest radius in the app, because it is the largest thing on the screen. */
internal val HeroCorner = RoundedCornerShape(
    topStart = 26.dp,
    topEnd = 26.dp,
    bottomStart = 26.dp,
    bottomEnd = 10.dp,
)

/** The slot's Material 3 shape set, so library components inherit the same geometry. */
val UnitWalkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = TileCorner,
    medium = SheetCorner,
    large = RoundedCornerShape(22.dp),
    extraLarge = HeroCorner,
)

/** The scaffold's name for the same set, kept so a caller written against the scaffold compiles. */
val PlayKilnShapes: Shapes = UnitWalkShapes

package com.gexetr.lawnroundlog.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// The slot's corner vocabulary. A mown lawn has soft swept edges and hard cut ones, and the shapes
// below say the same thing: everyday containers are generously rounded, and the states that are not
// "done" carry a cut or a squared corner so a row can be read by silhouette before it is read by
// colour - which is what the round-log spec asks for when it says a visit status must be stated with
// a word or a shape in addition to hue alone.

/** The corner scale `LawnRoundLogTheme` installs. Generous, because these are gloved thumbs. */
internal val RoundShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/**
 * A plot: three soft corners and one cut corner, the way a mown edge meets an unmown one.
 *
 * Used for the panels that hold a customer or a day, so the round reads as a set of plots rather
 * than as a stack of identical cards.
 */
val PlotCardShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 22.dp,
    bottomEnd = 22.dp,
    bottomStart = 8.dp,
)

/** A visit that has been mowed: the finished state, so the shape is the closed one - a full pill. */
val VisitMowedShape = RoundedCornerShape(percent = 50)

/** A visit that was skipped: two opposite corners chamfered, the shape of an edge left standing. */
val VisitSkippedShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)

/** A visit that was rained off: squared off, because nothing was done and nothing was decided. */
val VisitRainedOffShape = RoundedCornerShape(4.dp)

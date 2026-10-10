package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Lumintra's shapes carry the almanac voice: almost square, with the corner easing off only enough
// to read as a plate of glass rather than a table cell. The radius is deliberately small — a sky
// atlas is ruled, not bubbly — and the one large radius is reserved for the hero sky panel and the
// phase dial, which are the two surfaces drawn on the night field itself.
//
// Nothing here is a Material default, per .cursor/rules/visual-identity.mdc: a slot owns its shapes
// the way it owns its palette.
val LumintraShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

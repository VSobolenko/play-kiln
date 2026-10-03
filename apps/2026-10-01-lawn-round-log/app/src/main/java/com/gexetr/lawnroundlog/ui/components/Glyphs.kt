package com.gexetr.lawnroundlog.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gexetr.lawnroundlog.R

/**
 * The round log's UI iconography.
 *
 * These glyphs are **not drawn here**. They are the Tabler Icons set, vendored into
 * `app/src/main/res/drawable/` with its licence text and a per-icon provenance record, exactly as
 * `.cursor/rules/visual-identity.mdc` requires: general-purpose UI iconography — a back arrow, a
 * plus, a bin, a lock — comes from one licence-cleared professional set, because a set drawn by hand
 * from path primitives is not competing with a set drawn by a professional team. The owner's
 * instruction, recorded 2026-10-02, is that the factory takes a ready-made, well-chosen set.
 *
 * A file in this module that hand-rolls a general-purpose glyph with `Canvas`/`drawLine` is the
 * defect that rule names, not the virtue. What stays slot-owned is the **mark** — the launcher icon,
 * the adaptive-icon layers and the in-app brand glyph in `Brand.kt` — which rule 13 keeps out of the
 * library, and which is derived from `store/studio.json`.
 *
 * The one judgement each name records is whether the library expresses the app's own noun honestly.
 * `GlyphRain` is the set's own `cloud-rain` because a rained-off visit *is* weather; `GlyphLock` is
 * the set's lock because the paywall's shape is not the app's invention. A glyph whose meaning the
 * app defined and the set could not express would stay slot-owned here, drawn in this set's stroke
 * weight — none of the seventeen currently needs that.
 *
 * Colour never lives in the icon. Every glyph takes [tint], defaulting to `LocalContentColor.current`,
 * so the set inherits the slot's own theme and cannot drift from the palette in `ui/theme/Color.kt`;
 * the vendored `VectorDrawable`s carry a stroke colour only because a vector path must name one to
 * render at all, and Compose's `Icon` tint replaces it at every call site.
 *
 * Accessibility is the call site's decision, not the glyph's: a glyph that is the only content of a
 * control needs an accessible name, and one that merely repeats an adjacent label should stay
 * decorative. So none of these carry a `contentDescription`; the noun is supplied where the noun is
 * known, in the enclosing control's own semantics.
 *
 * Provenance: `apps/2026-10-01-lawn-round-log/icons/provenance.json`, licence text in
 * `apps/2026-10-01-lawn-round-log/icons/LICENSES/MIT.txt`.
 */

/** The box every glyph draws inside. 24 dp is the icon convention the rest of the UI is laid out to. */
private val GlyphSize = 24.dp

/**
 * The one drawing surface every glyph is composed on.
 *
 * Keeping the size and the tint derivation in one place is what makes the family consistent by
 * construction rather than by discipline: a glyph cannot pick its own size or its own colour,
 * because it is handed neither.
 */
@Composable
private fun Glyph(
    @DrawableRes art: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) = Icon(
    painter = painterResource(art),
    contentDescription = null,
    modifier = modifier.size(GlyphSize),
    tint = tint,
)

/** Back. */
@Composable
fun GlyphBack(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_arrow_left, modifier, tint)

/** Add. */
@Composable
fun GlyphAdd(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_plus, modifier, tint)

/** Delete. */
@Composable
fun GlyphDelete(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_trash, modifier, tint)

/** Edit. */
@Composable
fun GlyphEdit(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_pencil, modifier, tint)

/** Lock. The paywall's shape. */
@Composable
fun GlyphLock(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_lock, modifier, tint)

/** Export. A file leaving the app, which is the action rather than the destination it opens. */
@Composable
fun GlyphExport(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_file_export, modifier, tint)

/** Share. The same document handed to any app that accepts one. */
@Composable
fun GlyphShare(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_share, modifier, tint)

/** A customer. */
@Composable
fun GlyphCustomer(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_user, modifier, tint)

/** Move up the round. */
@Composable
fun GlyphMoveUp(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_arrow_up, modifier, tint)

/** Move down the round. */
@Composable
fun GlyphMoveDown(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_arrow_down, modifier, tint)

/** Mowed. */
@Composable
fun GlyphMowed(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_check, modifier, tint)

/** Skipped. */
@Composable
fun GlyphSkipped(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_x, modifier, tint)

/** Rained off. The weather, which is the reason, rather than a generic warning. */
@Composable
fun GlyphRain(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_cloud_rain, modifier, tint)

/** A warning, for the cases that are neither weather nor a plain skip. */
@Composable
fun GlyphWarning(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_alert_triangle, modifier, tint)

/** A date, for the round's own day. */
@Composable
fun GlyphDate(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_calendar, modifier, tint)

/** Settings. Sliders, which is what this screen's job is. */
@Composable
fun GlyphSettings(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_adjustments_horizontal, modifier, tint)

/** Information. */
@Composable
fun GlyphInfo(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) =
    Glyph(R.drawable.ic_info_circle, modifier, tint)

/**
 * The whole set on one surface.
 *
 * A glyph set is judged by whether it reads as one family, and that is not visible one glyph at a
 * time — so the preview shows the family, at the size the UI actually draws it.
 */
@Preview(showBackground = true)
@Composable
private fun GlyphSetPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphBack()
                    GlyphAdd()
                    GlyphDelete()
                    GlyphEdit()
                    GlyphLock()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphExport()
                    GlyphShare()
                    GlyphCustomer()
                    GlyphMoveUp()
                    GlyphMoveDown()
                    GlyphMowed()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlyphSkipped()
                    GlyphRain()
                    GlyphWarning()
                    GlyphDate()
                    GlyphSettings()
                    GlyphInfo()
                }
            }
        }
    }
}

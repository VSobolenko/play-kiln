package com.gexetr.lumintralightplanner.ui.targets

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import java.util.Locale

/**
 * One catalogue row's data, already filtered into the visible list.
 *
 * The row itself owns no ordering: the order is the catalogue's own
 * (`SkyObject.CATALOGUE_ORDER` — magnitude ascending, magnitude-unknown rows last, then by id) and is
 * applied once by `TargetsViewModel`. The per-night visibility that arrives here is a FILTER result,
 * never a ranking (spec: "The chosen night's visibility is a filter on the list, never a re-ordering").
 */
data class TargetRowModel(
    val id: String,
    val designation: String,
    val name: String,
    val nameRu: String?,
    val type: ObjectType,
    val magnitude: Double?,
    val magnitudeSystem: String?,
    val magnitudeKind: String?,
    val magnitudeDisputed: Boolean,
    val sizeMajArcmin: Double?,
    val sizeMinArcmin: Double?,
    val favourite: Boolean,
    /** `true` up tonight, `false` never up tonight, `null` when no spot/night is chosen. */
    val visibleTonight: Boolean?,
)

/** True when the active locale's language is Russian, which is when `nameRu` is preferred. */
private fun prefersRussian(): Boolean = Locale.getDefault().language == "ru"

/** The localised name of a path: `nameRu` when the locale is Russian and the catalogue carries one. */
fun displayName(model: TargetRowModel): String =
    if (prefersRussian() && !model.nameRu.isNullOrBlank()) model.nameRu else model.name

/** The localised label resource for an object type, shared by the row and the detail surface. */
@StringRes
fun objectTypeLabel(type: ObjectType): Int = when (type) {
    ObjectType.GALAXY -> R.string.target_type_galaxy
    ObjectType.GALAXY_GROUP -> R.string.target_type_galaxy_group
    ObjectType.GLOBULAR_CLUSTER -> R.string.target_type_globular_cluster
    ObjectType.OPEN_CLUSTER -> R.string.target_type_open_cluster
    ObjectType.EMISSION_NEBULA -> R.string.target_type_emission_nebula
    ObjectType.REFLECTION_NEBULA -> R.string.target_type_reflection_nebula
    ObjectType.PLANETARY_NEBULA -> R.string.target_type_planetary_nebula
    ObjectType.SUPERNOVA_REMNANT -> R.string.target_type_supernova_remnant
    ObjectType.DARK_NEBULA -> R.string.target_type_dark_nebula
    ObjectType.ASTERISM -> R.string.target_type_asterism
    ObjectType.DOUBLE_STAR -> R.string.target_type_double_star
    ObjectType.PLANET -> R.string.target_type_planet
    ObjectType.MOON -> R.string.target_type_moon
    ObjectType.SUN -> R.string.target_type_sun
}

/** The localised label resource for a `magKind` wire value; an unrecognised value reads as unknown. */
@StringRes
fun magnitudeKindLabel(kind: String?): Int = when (kind?.lowercase(Locale.ROOT)) {
    "integrated" -> R.string.target_mag_kind_integrated
    "peak" -> R.string.target_mag_kind_peak
    else -> R.string.target_mag_kind_unknown
}

/** The magnitude line: the value with its system and kind, or the stated unknown. */
@Composable
internal fun magnitudeText(model: TargetRowModel): String {
    val value = model.magnitude
    if (value == null) return stringResource(R.string.target_magnitude_unknown)
    val formatted = String.format(Locale.getDefault(), "%.1f", value)
    val system = model.magnitudeSystem
    val kind = model.magnitudeKind
    return if (system != null && kind != null) {
        stringResource(R.string.target_mag_line, formatted, system, stringResource(magnitudeKindLabel(kind)))
    } else {
        stringResource(R.string.target_filter_magnitude_value, formatted)
    }
}

/** The size line, or the stated unknown when the source carries no apparent size. */
@Composable
internal fun sizeText(model: TargetRowModel): String {
    val major = model.sizeMajArcmin ?: return stringResource(R.string.target_size_unknown)
    val formattedMajor = String.format(Locale.getDefault(), "%.1f", major)
    val minor = model.sizeMinArcmin
    return if (minor != null) {
        stringResource(R.string.target_size_pair, formattedMajor, String.format(Locale.getDefault(), "%.1f", minor))
    } else {
        stringResource(R.string.target_size_format, formattedMajor)
    }
}

/**
 * One row of the target catalogue.
 *
 * The favourite control is a real IconButton (48 dp, named for TalkBack) and the row is a separate
 * clickable target, so a tap aimed at "open the target" cannot silently toggle the favourite. A
 * favourited row is distinguished by a word as well as by the filled star, because colour and glyph
 * are not the only signal a user can read (spec: "a word or a shape in addition to colour alone").
 */
@Composable
fun TargetRow(
    model: TargetRowModel,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = displayName(model)
    val typeName = stringResource(objectTypeLabel(model.type))
    val mag = magnitudeText(model)
    val size = sizeText(model)
    val favouriteWord = if (model.favourite) stringResource(R.string.target_favourite_mark) else ""
    val rowDescription = buildString {
        append(model.designation)
        append(", ")
        append(name)
        append(", ")
        append(typeName)
        append(", ")
        append(mag)
        if (model.magnitudeDisputed) append(", ").append(stringResource(R.string.target_magnitude_disputed))
        append(", ")
        append(size)
        if (model.favourite) append(", ").append(favouriteWord)
        if (model.visibleTonight == true) append(", ").append(stringResource(R.string.target_up_tonight))
    }
    val favouriteDescription = stringResource(
        if (model.favourite) R.string.target_favourite_remove else R.string.target_favourite_add,
        name,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onOpen)
            .semantics(mergeDescendants = true) {
                contentDescription = rowDescription
                role = Role.Button
            },
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_crosshair),
                contentDescription = null,
                tint = LumintraTheme.colors.moonlight,
                modifier = Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.designation,
                        style = MaterialTheme.typography.labelLarge,
                        color = LumintraTheme.colors.firstLight,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.target_row_metrics, mag, size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                if (model.favourite) {
                    Text(
                        text = favouriteWord,
                        style = MaterialTheme.typography.labelSmall,
                        color = LumintraTheme.colors.darkWindow,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = favouriteDescription },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_star),
                    contentDescription = null,
                    tint = if (model.favourite) LumintraTheme.colors.firstLight else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

package com.gexetr.lumintralightplanner.domain

/**
 * The closed set of object types the catalogue and the computed appendix can name.
 *
 * The last three are used **only** by the computed Solar-System appendix and are never authored into
 * `objects.json`: a planet has no fixed RA/Dec, so it can only exist as a computed record.
 */
enum class ObjectType {
    GALAXY,
    GALAXY_GROUP,
    GLOBULAR_CLUSTER,
    OPEN_CLUSTER,
    EMISSION_NEBULA,
    REFLECTION_NEBULA,
    PLANETARY_NEBULA,
    SUPERNOVA_REMNANT,
    DARK_NEBULA,
    ASTERISM,
    DOUBLE_STAR,

    /** Computed only. */
    PLANET,

    /** Computed only. */
    MOON,

    /** Computed only. */
    SUN;

    /** The stable wire name used in `objects.json`. */
    val wireName: String get() = name.lowercase()

    /** True when the type may only be produced by the computed appendix, never authored. */
    val isComputedOnly: Boolean get() = this == PLANET || this == MOON || this == SUN

    companion object {
        val AUTHORED_TYPES: List<ObjectType> = entries.filterNot { it.isComputedOnly }

        fun fromWire(value: String): ObjectType? = entries.firstOrNull { it.wireName == value }
    }
}

/** Whether a row is an authored catalogue entry or computed for the instant. */
enum class ObjectKind {
    /** A fixed-coordinate row from `objects.json` (or a star). */
    FIXED,

    /** A Solar-System body computed at the observation instant. */
    COMPUTED,
}

/**
 * How a target stands relative to the effective horizon — the user's drawn horizon raised by the
 * altitude mask (default 10.0°, user-adjustable).
 */
enum class ObjectVisibility {
    /** Above the effective horizon for the whole session window. */
    CIRCUMPOLAR,

    /** Crosses the effective horizon: up for part of the session. */
    ABOVE,

    /** Never reaches the effective horizon at this latitude. */
    BELOW,

    /** The mask is above the target's maximum altitude, so it is never usable from this spot. */
    NEVER_UP,
}

/** Which reference supplied which identifier, recorded so the row's provenance is auditable. */
data class SourceIds(
    val wikidata: String? = null,
    val heasarc: String? = null,
    val simbad: String? = null,
)

/**
 * The two independent sources a coordinate was checked against, and the measured disagreement.
 *
 * [deltaArcmin] is computed as `(180/π)·acos(sin δ₁ sin δ₂ + cos δ₁ cos δ₂ cos(α₁ − α₂))`. Acceptance
 * is `Δ ≤ 1.0 arcmin` for objects with `sizeMajArcmin ≥ 5` and `Δ ≤ 0.2 arcmin` for unresolved ones;
 * a row outside tolerance is re-sourced or dropped, **never averaged**.
 */
data class RowVerification(
    val srcA: String,
    val srcB: String,
    val deltaArcmin: Double,
)

/**
 * One row of the deep-sky catalogue, exactly as `assets/catalog/objects.json` declares it.
 *
 * The format is ICRS/J2000.0 and [raDeg] is **degrees**, never hours. Nothing here is derived at
 * authoring time: [raHours] exists only so the engine adapter has one named conversion to make.
 */
data class SkyObject(
    val id: String,
    val messier: String? = null,
    val ngc: String? = null,
    val ic: String? = null,
    val name: String,
    val nameRu: String? = null,
    val type: ObjectType,
    val raDeg: Double,
    val decDeg: Double,
    val epoch: String = EquatorialCoords.EPOCH,
    val magV: Double? = null,
    val magB: Double? = null,
    /** Which of `magV` / `magB` is authoritative, or `null` when neither is known. */
    val magSystem: String? = null,
    /** `integrated`, `peak` or `unknown`. Never inferred, always stated by the source. */
    val magKind: String? = null,
    val sizeMajArcmin: Double? = null,
    val sizeMinArcmin: Double? = null,
    val positionAngleDeg: Double? = null,
    val aliases: List<String> = emptyList(),
    val commonNames: List<String> = emptyList(),
    val sourceIds: SourceIds = SourceIds(),
    val verification: RowVerification? = null,
    /** Two sources disagreed by more than 0.50 mag; both values live in [notes]. Never averaged. */
    val magDisputed: Boolean = false,
    val notes: String? = null,
    val kind: ObjectKind = ObjectKind.FIXED,
    /** For a computed record only: the instant the RA/Dec were computed for. */
    val computedAtUtc: java.time.Instant? = null,
) {
    val equatorial: EquatorialCoords get() = EquatorialCoords(raDeg, decDeg)

    /** The engine's RA unit, named so a call site cannot confuse it with [raDeg]. */
    val raHours: Double get() = raDeg / 15.0

    /** The magnitude the list sorts on: V when known, else B, else unknown. */
    val displayMagnitude: Double? get() = magV ?: magB

    /** True when the row carries no magnitude at all, which sorts it last. */
    val hasUnknownMagnitude: Boolean get() = displayMagnitude == null

    /** The apparent size in degrees, or `null` when the source states none. */
    val angularSizeMajDeg: Double? get() = sizeMajArcmin?.let { it / 60.0 }

    /** True for a point source: no stated size, so no disc and no fit statement may be invented. */
    val isPointSource: Boolean get() = sizeMajArcmin == null || sizeMajArcmin <= 0.0

    /** The designation shown beside the name: `M31`, `NGC 7000`, else the id. */
    val designation: String
        get() = messier?.let { "M$it" } ?: ngc?.let { "NGC $it" } ?: ic?.let { "IC $it" } ?: id

    /** Every string a search query may match: designations, names and aliases, case-insensitively. */
    val searchableTerms: List<String>
        get() = buildList {
            add(id)
            add(name)
            nameRu?.let { add(it) }
            messier?.let { add("m$it"); add("messier $it") }
            ngc?.let { add("ngc$it"); add("ngc $it") }
            ic?.let { add("ic$it"); add("ic $it") }
            addAll(aliases)
            addAll(commonNames)
        }

    /**
     * The one comparator the catalogue's order is defined by: **magnitude ascending, magnitude-unknown
     * rows last, then by `id`**. It is the list's order and never a per-night ranking.
     */
    companion object {
        val CATALOGUE_ORDER: Comparator<SkyObject> = compareBy<SkyObject> { it.displayMagnitude ?: Double.MAX_VALUE }
            .thenBy { it.id }

        /** Applies [searchableTerms] as the case-insensitive contains test the search field uses. */
        fun matchesQuery(object0: SkyObject, query: String): Boolean {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return true
            return object0.searchableTerms.any { it.lowercase().contains(q) }
        }
    }
}

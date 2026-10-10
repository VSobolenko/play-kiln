package com.gexetr.lumintralightplanner.data

import android.content.Context
import com.gexetr.lumintralightplanner.domain.EquatorialCoords
import com.gexetr.lumintralightplanner.domain.GeodeticPoint
import com.gexetr.lumintralightplanner.domain.ObjectKind
import com.gexetr.lumintralightplanner.domain.ObjectType
import com.gexetr.lumintralightplanner.domain.RowVerification
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.SourceIds
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * One IAU constellation region, exactly as `assets/catalog/constellations.json` declares it.
 *
 * A constellation is a **region, not a point**, and it is never a target: it has no rise/set window,
 * it is never ranked by magnitude, and it is never filtered as an object. The centre coordinates are
 * the region's centre, which is what the app uses to *label* the constellation an aim point falls in
 * — the asset carries no boundary polygon, so no boundary test is possible from this data (see
 * [CatalogueRepository.nearestConstellation]).
 */
data class ConstellationRecord(
    val id: String,
    val nameEn: String,
    val nameRu: String,
    val nameLatin: String,
    val abbrev: String,
    val raCenterDeg: Double,
    val decCenterDeg: Double,
    val areaRank: Int,
)

/**
 * What `objects.json` says about itself, so the selection rule's output is auditable rather than
 * implied — "about 250" is not a checkable number.
 *
 * Every field but [authoredRowCount] is optional: the manifest block is authored by whoever bakes the
 * table, and this repository must load a table whose manifest is absent (the rows are the product; the
 * manifest is the receipt) while still recording the measurement it can take itself.
 */
data class CatalogueManifest(
    val schemaVersion: Int?,
    /** The count the manifest claims. Compared with the rows actually loaded. */
    val declaredRowCount: Int?,
    /** The non-Messier cap the author applied (140 at the declared limit). */
    val nonMessierCap: Int?,
    /** The magnitude limit actually used (`10.0`, or `10.5` when 10.0 supplied fewer than 140). */
    val magnitudeLimit: Double?,
    /** The rows this repository actually loaded and accepted. */
    val authoredRowCount: Int,
    /** The computed Solar-System records the appendix must supply. */
    val computedBodyCount: Int,
) {
    /** False when the manifest's own count disagrees with the table it describes. */
    val countAgrees: Boolean get() = declaredRowCount == null || declaredRowCount == authoredRowCount
}

/**
 * What reading the catalogue produced.
 *
 * The asset is either usable or it is not; there is no third partial state, because a half-loaded
 * catalogue would silently answer "never rises" for the rows it dropped. Rows that fail the schema
 * while the document itself parses are **counted** ([Success.rejectedRowCount]) rather than silently
 * discarded, so the defect is visible to whoever reads the report.
 */
sealed interface CatalogueResult {

    data class Success(
        /** The authored deep-sky rows, in [SkyObject.CATALOGUE_ORDER]. */
        val objects: List<SkyObject>,
        val constellations: List<ConstellationRecord>,
        /** True when `constellations.json` is absent or unreadable; the objects still load. */
        val constellationsUnavailable: Boolean,
        val manifest: CatalogueManifest,
        /** The FK6 ⊕ supplement star table, when `assets/catalog/stars.bin` is packaged. */
        val starTable: BundledAsset<StarTable>,
        /** Rows present in the document but rejected for a schema violation. */
        val rejectedRowCount: Int,
    ) : CatalogueResult

    /**
     * The catalogue asset is missing or unreadable. The app still starts; the `Targets` screen shows
     * its `target_list_unavailable` state and the brief omits targets and says so.
     */
    data class Unreadable(
        val reason: CatalogueUnreadableReason,
        /** A technical diagnostic for logs. Never a user-facing sentence. */
        val detail: String,
    ) : CatalogueResult
}

/** Why the authored catalogue could not be loaded. The UI maps every value to one sentence. */
enum class CatalogueUnreadableReason {
    NOT_PACKAGED,
    READ_FAILED,
    NOT_JSON,
    MISSING_OBJECTS_ARRAY,
    NO_USABLE_ROWS,
    NOT_AN_OBJECT,
}

/**
 * One Solar-System body as the ephemeris computed it for an instant and a place.
 *
 * The values themselves are **injected**: the `astronomy.kt` port is the only engine that computes
 * Sun/Moon/planet positions, and this file must not drag VSOP87/ELP into a unit test. The provider
 * answers with geometry; [CatalogueRepository.computedSolarSystem] turns that into the computed
 * catalogue records.
 *
 * [name] and [nameRu] come from the provider because the body names are user-facing and this layer
 * carries no strings — `ru` is a declared locale, so each body needs a hand-authored Russian name
 * exactly as an authored catalogue row does.
 */
data class EphemerisSample(
    /** A stable id: `"sun"`, `"moon"`, `"planet-mars"`. */
    val id: String,
    val type: ObjectType,
    val name: String,
    val nameRu: String? = null,
    /** Geocentric apparent right ascension, degrees `[0, 360)`. */
    val raDeg: Double,
    /** Geocentric apparent declination, degrees `[-90, +90]`. */
    val decDeg: Double,
    /** Altitude above the geometric horizon, degrees, from `horizon(..., Refraction.Normal)`. */
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    /** Apparent magnitude from `illumination(body, t).mag`, or `null` when the engine gives none. */
    val apparentMagnitude: Double? = null,
    /** Phase angle in degrees from `illumination(body, t).phaseAngle`. */
    val phaseAngleDeg: Double? = null,
    /** Illuminated fraction `[0, 1]` from `illumination(body, t).phaseFraction`. */
    val illuminatedFraction: Double? = null,
    /** Angular separation from the Sun in degrees; the "behind the Sun" test reads it. */
    val angularSeparationFromSunDeg: Double? = null,
    /** Apparent major axis in arcminutes, when the engine states one. */
    val angularSizeArcmin: Double? = null,
)

/**
 * The ephemeris boundary. Implemented by the `astronomy.kt` adapter, which owns the RA hours↔degrees
 * conversion at exactly one place (`raHours = raDeg / 15`), never here.
 */
interface SolarSystemEphemerisProvider {
    /** Every Solar-System body the appendix must present, for [instantUtc] seen from [observer]. */
    fun bodies(instantUtc: Instant, observer: GeodeticPoint): List<EphemerisSample>
}

/**
 * Where a computed body stands relative to the user's own horizon and to the Sun.
 *
 * Both non-visible cases are **stated with their numbers** — a below-horizon body keeps its altitude
 * and a behind-the-Sun body keeps its separation — because hiding or blanking the record is the
 * defect the contract forbids.
 */
enum class ComputedBodyState {
    /** Above the altitude mask: usable from this spot at this instant. */
    ABOVE_HORIZON,

    /** Below the mask: still listed, with its computed (negative or low) altitude. */
    BELOW_HORIZON,

    /** Too close to the Sun to be usefully observable: listed with the separation that says so. */
    BEHIND_THE_SUN,
}

/**
 * A Solar-System record, presented with the same view fields a catalogue row uses plus the computed
 * facts, and carrying `ephemeris = "computed"` so the UI can tell it from an authored row.
 *
 * The apparent magnitude is deliberately **not** written into [skyObject]'s `magV`: a planet's
 * magnitude is not a V-system catalogue value, and putting it there is exactly how a V value would
 * end up compared with a B value. It lives here, beside the phase facts it belongs with.
 */
data class ComputedSkyObject(
    val skyObject: SkyObject,
    val altitudeDeg: Double,
    val azimuthDeg: Double,
    val apparentMagnitude: Double?,
    val phaseAngleDeg: Double?,
    val illuminatedFraction: Double?,
    val separationFromSunDeg: Double?,
    val state: ComputedBodyState,
) {
    val id: String get() = skyObject.id

    val type: ObjectType get() = skyObject.type

    val kind: ObjectKind get() = skyObject.kind

    /** The marker the spec names, so a computed record is never mistaken for an authored one. */
    val ephemeris: String get() = EPHEMERIS_COMPUTED

    companion object {
        const val EPHEMERIS_COMPUTED: String = "computed"
    }
}

/**
 * The computed Solar-System appendix: the records, plus the ids the contract requires that the
 * provider did not return. [missingBodyIds] being non-empty is a real gap in the adapter, and it is
 * reported rather than silently absent from the list.
 */
data class ComputedAppendix(
    val bodies: List<ComputedSkyObject>,
    val missingBodyIds: List<String>,
)

/**
 * Reads the app's authored catalogue from its packaged assets and builds the computed Solar-System
 * appendix. It makes **no network call of any kind** — the catalogue is the offline half of the
 * product, and that is its whole point.
 *
 * Assets read (paths relative to `app/src/main/assets/`):
 *
 * - `catalog/objects.json` — `{"schemaVersion": 1, "manifest": {…}, "objects": […]}`. The manifest
 *   block is optional and describes the selection rule's output; the objects array is required.
 * - `catalog/constellations.json` — `{"schemaVersion": 1, "constellations": […]}` with 88 rows.
 * - `catalog/stars.bin` — the FK6 ⊕ supplement star table, **11 B per star**, read through
 *   [SkyAssetSource] so exactly one parser exists for it.
 *
 * Every read is off the main thread; every failure is a typed value; nothing throws into the UI.
 */
class CatalogueRepository(private val context: Context) {

    private val assets = SkyAssetSource(context)

    /**
     * Loads the authored catalogue. The `suspend` shape is deliberate even though the work is local:
     * asset reads are blocking I/O, and a caller that can await a load cannot accidentally do it on
     * the main thread.
     */
    suspend fun load(): CatalogueResult = withContext(Dispatchers.IO) {
        val text = when (val read = assets.readText(OBJECTS_PATH)) {
            is BundledAsset.Present -> read.value
            is BundledAsset.Absent -> return@withContext CatalogueResult.Unreadable(
                reason = if (read.reason == AssetAbsenceReason.NOT_PACKAGED) {
                    CatalogueUnreadableReason.NOT_PACKAGED
                } else {
                    CatalogueUnreadableReason.READ_FAILED
                },
                detail = "objects.json: ${read.reason}",
            )
            is BundledAsset.Malformed -> return@withContext CatalogueResult.Unreadable(
                reason = CatalogueUnreadableReason.READ_FAILED,
                detail = "objects.json: ${read.reason}",
            )
        }

        val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
            ?: return@withContext CatalogueResult.Unreadable(
                CatalogueUnreadableReason.NOT_JSON,
                "objects.json is not valid JSON",
            )
        val rootObject = root as? JsonObject
            ?: return@withContext CatalogueResult.Unreadable(
                CatalogueUnreadableReason.NOT_AN_OBJECT,
                "objects.json root is not a JSON object",
            )
        val array = rootObject["objects"] as? JsonArray
            ?: return@withContext CatalogueResult.Unreadable(
                CatalogueUnreadableReason.MISSING_OBJECTS_ARRAY,
                "objects.json has no `objects` array",
            )

        val accepted = ArrayList<SkyObject>(array.size)
        var rejected = 0
        for (element in array) {
            val row = element as? JsonObject
            if (row == null) {
                rejected++
                continue
            }
            val parsed = parseObject(row)
            if (parsed == null) rejected++ else accepted.add(parsed)
        }
        if (accepted.isEmpty()) {
            return@withContext CatalogueResult.Unreadable(
                CatalogueUnreadableReason.NO_USABLE_ROWS,
                "objects.json carried ${array.size} rows and none passed the schema",
            )
        }
        accepted.sortWith(SkyObject.CATALOGUE_ORDER)

        val manifestBlock = rootObject["manifest"] as? JsonObject
        val manifest = CatalogueManifest(
            schemaVersion = rootObject.int("schemaVersion"),
            declaredRowCount = manifestBlock?.int("shippedRowCount")
                ?: manifestBlock?.int("rowCount")
                ?: rootObject.int("shippedRowCount"),
            nonMessierCap = manifestBlock?.int("nonMessierCap"),
            magnitudeLimit = manifestBlock?.double("magnitudeLimit"),
            authoredRowCount = accepted.size,
            computedBodyCount = REQUIRED_COMPUTED_BODY_IDS.size,
        )

        val constellations = readConstellations()
        CatalogueResult.Success(
            objects = accepted,
            constellations = constellations.records,
            constellationsUnavailable = constellations.unavailable,
            manifest = manifest,
            starTable = assets.starTable(),
            rejectedRowCount = rejected,
        )
    }

    /** The constellation figures from `assets/sky/constellation_lines.json`, or a typed absence. */
    fun constellationFigures(): BundledAsset<ConstellationFigures> = assets.constellationFigures()

    /**
     * The constellation whose **centre** is nearest to a direction, for labelling the aim point.
     *
     * This is honest about what it is: the shipped asset carries a region centre, not the IAU
     * boundary polygons, so this is a nearest-centre label and **not** a point-in-region test. A
     * constellation whose centre is far away can still own a nearby sky, and the reverse; the label
     * is therefore a convenience, and nothing in the product may depend on it being a boundary.
     */
    fun nearestConstellation(
        constellations: List<ConstellationRecord>,
        raDeg: Double,
        decDeg: Double,
    ): ConstellationRecord? = constellations.minByOrNull { candidate ->
        val dRa = Math.toRadians(candidate.raCenterDeg - raDeg)
        val d1 = Math.toRadians(candidate.decCenterDeg)
        val d2 = Math.toRadians(decDeg)
        val cosSep = Math.sin(d1) * Math.sin(d2) + Math.cos(d1) * Math.cos(d2) * Math.cos(dRa)
        Math.acos(cosSep.coerceIn(-1.0, 1.0))
    }

    /**
     * Builds the computed Solar-System appendix for one instant and one place.
     *
     * [altitudeMaskDeg] is the effective horizon the caller is using (the user's drawn horizon raised
     * by the altitude mask), and [behindSunMaxSeparationDeg] is the separation below which a body is
     * reported behind the Sun. Both are **product thresholds**, stated as parameters rather than
     * buried, because neither is a fact the ephemeris supplies.
     *
     * A body below the mask and a body behind the Sun are both still returned, each with the state
     * and the number that says so — never hidden, never blanked, never given a stale coordinate.
     */
    fun computedSolarSystem(
        instantUtc: Instant,
        observer: GeodeticPoint,
        ephemeris: SolarSystemEphemerisProvider,
        altitudeMaskDeg: Double = DEFAULT_ALTITUDE_MASK_DEG,
        behindSunMaxSeparationDeg: Double = DEFAULT_BEHIND_SUN_SEPARATION_DEG,
    ): ComputedAppendix {
        val samples = ephemeris.bodies(instantUtc, observer)
        val byId = LinkedHashMap<String, EphemerisSample>(samples.size)
        for (sample in samples) {
            // A body whose type is not computed-only is a provider defect, and `objects.json` is the
            // only place a fixed row may live; refuse it rather than present it as computed.
            if (!sample.type.isComputedOnly) continue
            byId[sample.id] = sample
        }
        val bodies = ArrayList<ComputedSkyObject>(byId.size)
        for (id in REQUIRED_COMPUTED_BODY_IDS) {
            val sample = byId[id] ?: continue
            bodies.add(toComputed(sample, instantUtc, altitudeMaskDeg, behindSunMaxSeparationDeg))
        }
        // Any computed body the provider supplies beyond the required ten is still presented.
        for ((id, sample) in byId) {
            if (id in REQUIRED_COMPUTED_BODY_IDS) continue
            bodies.add(toComputed(sample, instantUtc, altitudeMaskDeg, behindSunMaxSeparationDeg))
        }
        val missing = REQUIRED_COMPUTED_BODY_IDS.filterNot { byId.containsKey(it) }
        return ComputedAppendix(bodies, missing)
    }

    private fun toComputed(
        sample: EphemerisSample,
        instantUtc: Instant,
        altitudeMaskDeg: Double,
        behindSunMaxSeparationDeg: Double,
    ): ComputedSkyObject {
        val separation = sample.angularSeparationFromSunDeg
        val state = when {
            separation != null && separation <= behindSunMaxSeparationDeg -> ComputedBodyState.BEHIND_THE_SUN
            sample.altitudeDeg < altitudeMaskDeg -> ComputedBodyState.BELOW_HORIZON
            else -> ComputedBodyState.ABOVE_HORIZON
        }
        val skyObject = SkyObject(
            id = sample.id,
            name = sample.name,
            nameRu = sample.nameRu,
            type = sample.type,
            raDeg = sample.raDeg,
            decDeg = sample.decDeg,
            epoch = EquatorialCoords.EPOCH,
            sizeMajArcmin = sample.angularSizeArcmin,
            kind = ObjectKind.COMPUTED,
            computedAtUtc = instantUtc,
        )
        return ComputedSkyObject(
            skyObject = skyObject,
            altitudeDeg = sample.altitudeDeg,
            azimuthDeg = sample.azimuthDeg,
            apparentMagnitude = sample.apparentMagnitude,
            phaseAngleDeg = sample.phaseAngleDeg,
            illuminatedFraction = sample.illuminatedFraction,
            separationFromSunDeg = separation,
            state = state,
        )
    }

    /**
     * Parses one authored row, or returns `null` when it does not satisfy the schema.
     *
     * A null is a **rejection with a reason the manifest counts**, never a defaulted field: a row with
     * an invented magnitude or an averaged coordinate is worse than a missing row, which is why the
     * required set is enforced here rather than trusted.
     *
     * **The line this reader draws, stated rather than implied.** It rejects a row whose *identity or
     * geometry* cannot be read — `id`, `name`, `type`, `raDeg`, `decDeg`, and an `epoch` that is not
     * `J2000.0` — because such a row cannot be placed in the sky at all. The remaining required fields
     * are tolerated as absent and read as the **unknown** the domain type already models (`magV` null,
     * `verification` null, `aliases` empty), because their absence degrades a fact the row still has
     * rather than the row's existence: showing `M31` with its coordinate and an unknown magnitude beats
     * dropping `M31`. The spec's *"no row SHALL omit a required field"* is a **build-time** assertion
     * — the same spec says "a build-time test SHALL assert the asset is packaged and parses" — and a
     * runtime reader that silently emptied the Targets screen over a missing `nameLatin` would be
     * enforcing the rule in the one place it costs the user the whole feature.
     */
    private fun parseObject(row: JsonObject): SkyObject? {
        val id = row.string("id")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val name = row.string("name")?.takeIf { it.isNotBlank() } ?: return null
        val typeWire = row.string("type") ?: return null
        val type = ObjectType.fromWire(typeWire) ?: return null
        // The computed appendix is the only producer of planet/moon/sun; an authored row may not be one.
        if (type.isComputedOnly) return null
        val raDeg = row.double("raDeg") ?: return null
        val decDeg = row.double("decDeg") ?: return null
        if (!raDeg.isFinite() || !decDeg.isFinite()) return null
        if (decDeg < -90.0 || decDeg > 90.0) return null
        val epoch = row.string("epoch") ?: EquatorialCoords.EPOCH
        if (epoch != EquatorialCoords.EPOCH) return null

        val sourceIdsBlock = row["sourceIds"] as? JsonObject
        val verificationBlock = row["verification"] as? JsonObject
        val verification = verificationBlock?.let { block ->
            val srcA = block.string("srcA") ?: return@let null
            val srcB = block.string("srcB") ?: return@let null
            val delta = block.double("deltaArcmin") ?: return@let null
            RowVerification(srcA, srcB, delta)
        }

        return SkyObject(
            id = id,
            messier = row.string("messier"),
            ngc = row.string("ngc"),
            ic = row.string("ic"),
            name = name,
            nameRu = row.string("nameRu"),
            type = type,
            raDeg = normaliseDegrees360(raDeg),
            decDeg = decDeg,
            epoch = epoch,
            magV = row.double("magV"),
            magB = row.double("magB"),
            magSystem = row.string("magSystem"),
            magKind = row.string("magKind"),
            sizeMajArcmin = row.double("sizeMajArcmin"),
            sizeMinArcmin = row.double("sizeMinArcmin"),
            positionAngleDeg = row.double("positionAngleDeg"),
            aliases = row.stringList("aliases"),
            commonNames = row.stringList("commonNames"),
            sourceIds = SourceIds(
                wikidata = sourceIdsBlock?.string("wikidata"),
                heasarc = sourceIdsBlock?.string("heasarc"),
                simbad = sourceIdsBlock?.string("simbad"),
            ),
            verification = verification,
            magDisputed = row.bool("magDisputed") ?: false,
            notes = row.string("notes"),
            kind = ObjectKind.FIXED,
            computedAtUtc = null,
        )
    }

    private fun readConstellations(): ConstellationRead {
        val text = when (val read = assets.readText(CONSTELLATIONS_PATH)) {
            is BundledAsset.Present -> read.value
            is BundledAsset.Absent -> return ConstellationRead(emptyList(), unavailable = true)
            is BundledAsset.Malformed -> return ConstellationRead(emptyList(), unavailable = true)
        }
        val root = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return ConstellationRead(emptyList(), unavailable = true)
        val array = root["constellations"] as? JsonArray
            ?: return ConstellationRead(emptyList(), unavailable = true)
        val records = ArrayList<ConstellationRecord>(array.size)
        for (element in array) {
            val row = element as? JsonObject ?: continue
            val abbrev = row.string("abbrev")?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val id = row.string("id")?.trim()?.takeIf { it.isNotEmpty() } ?: abbrev
            val raCenter = row.double("raCenterDeg") ?: continue
            val decCenter = row.double("decCenterDeg") ?: continue
            if (!raCenter.isFinite() || !decCenter.isFinite()) continue
            if (decCenter < -90.0 || decCenter > 90.0) continue
            records.add(
                ConstellationRecord(
                    id = id,
                    nameEn = row.string("nameEn") ?: abbrev,
                    nameRu = row.string("nameRu") ?: row.string("nameEn") ?: abbrev,
                    nameLatin = row.string("nameLatin") ?: abbrev,
                    abbrev = abbrev,
                    raCenterDeg = normaliseDegrees360(raCenter),
                    decCenterDeg = decCenter,
                    areaRank = row.int("areaRank") ?: 0,
                ),
            )
        }
        return ConstellationRead(records, unavailable = records.isEmpty())
    }

    private class ConstellationRead(val records: List<ConstellationRecord>, val unavailable: Boolean)

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val OBJECTS_PATH: String = "catalog/objects.json"
        const val CONSTELLATIONS_PATH: String = "catalog/constellations.json"

        /** Where the deep-sky table and the computed appendix are both required to exist. */
        val STAR_TABLE_PATH: String get() = SkyAssetSource.STAR_TABLE_PATH

        /**
         * The Solar-System bodies the appendix must present, in presentation order. Ten records:
         * the Sun, the Moon, Mercury…Neptune and Pluto. Pluto is optional in the design but the
         * appendix names it, and [ComputedAppendix.missingBodyIds] reports it honestly when the
         * adapter does not supply it.
         */
        val REQUIRED_COMPUTED_BODY_IDS: List<String> = listOf(
            "sun",
            "moon",
            "planet-mercury",
            "planet-venus",
            "planet-mars",
            "planet-jupiter",
            "planet-saturn",
            "planet-uranus",
            "planet-neptune",
            "planet-pluto",
        )

        /** The effective-horizon default the targets capability also uses. */
        const val DEFAULT_ALTITUDE_MASK_DEG: Double = SatellitePass.DEFAULT_MASK_DEG

        /**
         * Below this solar elongation a body is reported behind the Sun. A **product threshold**,
         * chosen so "behind the Sun" means the geometry the user cannot shoot through rather than a
         * near-conjunction at which a low target may still be visible.
         */
        const val DEFAULT_BEHIND_SUN_SEPARATION_DEG: Double = 12.0
    }
}

// ---------------------------------------------------------------------------------------------
// Typed JSON access. Every accessor distinguishes a missing key from a present-but-wrong type,
// because a silently defaulted field is how a fabricated catalogue row is born.
// ---------------------------------------------------------------------------------------------

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.double(key: String): Double? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

private fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

private fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
        ?: emptyList()

/** Right ascension is stored in degrees in `[0, 360)`, so a value from a reference is wrapped once. */
private fun normaliseDegrees360(value: Double): Double {
    val m = value % 360.0
    return if (m < 0.0) m + 360.0 else m
}

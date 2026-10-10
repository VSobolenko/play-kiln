package com.gexetr.lumintralightplanner.data

import com.gexetr.lumintralightplanner.domain.EquipmentProfile
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.Scout
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The versioned, framework-free export/import codec for the user's scout setup.
 *
 * **No Android type appears on this file's classpath.** It depends only on
 * `kotlinx.serialization.json`, the pure-Kotlin `domain/` types ([Scout], [EquipmentProfile],
 * [HorizonProfile]) and `java.time`, which is why it is exercised by ordinary JVM unit tests
 * ([SpotTransferTest]) rather than an instrumented run. [ScoutStore] is the only Android caller, and it
 * delegates every byte of parsing here rather than re-implementing it.
 *
 * The discipline this file exists to enforce (`design.md` § Persistence "The codec discipline",
 * `specs/scout-spots` *"The scout store persists, writes atomically, and its codec never throws"* and
 * *"Import merges and refuses rather than corrupting"*):
 *
 * - **[decodeDocument] never throws.** A malformed, truncated or crafted document is a typed
 *   [DecodeResult.Refused], never an exception into the UI — a cold start must cost the file, never the
 *   app.
 * - **The numeric horizon codec has exactly one home.** The 360 little-endian `int16` samples and their
 *   tenths-of-a-degree clamp live in `HorizonProfile.encode` / `HorizonProfile.decode`; this file only
 *   translates between the document's integer array and that buffer, and never re-rounds or re-clamps.
 * - **The active spot is not here.** The document carries no active selection, so the importing device
 *   derives its own ([ScoutStore] does that with `ScoutSelection`).
 *
 * The written document is `lumintra-scout-<yyyyMMdd>.json` ([exportFileName]), UTF-8, pretty-printed so
 * a human can read it. Callers persist that document through `ACTION_CREATE_DOCUMENT` (or a
 * `FileProvider` share) so the user owns the file; this codec never opens an Intent itself. It
 * carries **exactly** the fields of `specs/scout-spots` *"The user's spots,
 * horizons and equipment export to a file they own"* and no others:
 *
 * ```
 * schemaVersion (first) | app | exportedAtUtc | spots[] | equipmentProfiles[] | favouriteTargetIds[]
 * spots[]:         id | name | latDeg | lonDeg | elevationM | tzId | note | equipmentProfileId | horizonProfile
 * horizonProfile:  sampleCount | azimuthStepDeg | elevationUnitDeg | samples
 * equipmentProfiles[]: id | name | focalLengthMm | sensorWidthMm | sensorHeightMm
 * ```
 *
 * **The document is not the store.** It is narrower than what [ScoutStore] keeps on disk: it carries no
 * session data, no caches, no element sets and no forecast, and it deliberately carries no `approximate`
 * flag and no `presetName` because the spec's field list has no key for them (see [encodeDocument] and
 * the class KDoc of [SpotRecordDto] for the two losses this states rather than hides).
 */
object SpotTransfer {

    /** The schema this build writes and the newest one it can read. */
    const val SUPPORTED_SCHEMA_VERSION: Int = 1

    /** The oldest schema this build still understands; anything older is not a scout file. */
    const val MIN_SCHEMA_VERSION: Int = 1

    /** The document's writer tag, so a foreign JSON file fails as not-a-scout-file rather than parsing. */
    const val APP_TAG: String = "lumintra"

    /** The export file name stem; the caller appends `<yyyyMMdd>.json`. */
    const val EXPORT_FILE_PREFIX: String = "lumintra-scout-"

    /**
     * Hard caps, validated **before** any value is allocated from the document.
     *
     * [MAX_DOCUMENT_CHARS] is the load-bearing one. `kotlinx.serialization` parses a `String` into a
     * tree, so *the only bound that exists before allocation is the length of the text*: a document
     * declaring a 10,000,000-element `samples` array would already be allocated by the parser, so the
     * guard that makes "caps are validated before allocation" true is the length cap checked on the raw
     * text, **before** the parse. The per-record caps below then bound what a document of legal length
     * may ask for. Arithmetic behind [MAX_DOCUMENT_CHARS] (`INFERRED`, from the writer's own output
     * shape): one 360-sample array pretty-printed at this nesting is at most `18` characters per sample
     * (`12` indent + up to `5` for `-100,` + newline), so the writer's largest legal document is
     * `500 × (360 × 18 + ~500) ≈ 3.6 M` characters; the cap sits above that and far below the hundreds
     * of megabytes a declared-length allocation would need.
     */
    const val MAX_DOCUMENT_CHARS: Int = 4_000_000
    const val MAX_SPOTS: Int = 500
    const val MAX_EQUIPMENT_PROFILES: Int = 500
    const val MAX_FAVOURITES: Int = 2_000
    const val MAX_NAME_LENGTH: Int = 200
    const val MAX_ID_LENGTH: Int = 128
    const val MAX_TZ_LENGTH: Int = 64
    const val MAX_NOTE_LENGTH: Int = 500

    private const val MAX_DIAGNOSTIC_LENGTH: Int = 160
    private const val HORIZON_AZIMUTH_STEP_DEG: Double = 1.0
    private const val HORIZON_ELEVATION_UNIT_DEG: Double = 0.1

    private val FILE_DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

    /**
     * The json codec. `prettyPrint` makes the exported document the human-readable file the spec
     * requires; `encodeDefaults` and `explicitNulls` write **every declared key**, including
     * `"horizonProfile": null` (the `hasProfile = false` state) and `"note": null`, so a reader sees the
     * fields the contract names rather than inferring them from an omission; `ignoreUnknownKeys` lets a
     * later document carry extra keys without this build refusing it before it has read `schemaVersion`.
     */
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
        isLenient = false
    }

    // ---------------------------------------------------------------------------------------------
    // Transfer types
    // ---------------------------------------------------------------------------------------------

    /** How an import treats records that already exist in the store. */
    enum class ImportMode {
        /** Skip a record whose name and coordinates match an existing record exactly; add the rest. */
        MERGE,

        /** Replace every saved spot with the document's spots; favourites are still unioned. */
        REPLACE_ALL,
    }

    /** Every typed way a document can be refused; the UI maps each to its own resource. */
    enum class SpotTransferErrorCode {
        EMPTY_DOCUMENT,
        NOT_A_SCOUT_FILE,
        UNKNOWN_SCHEMA_VERSION,
        TOO_LARGE,
        TOO_MANY_SPOTS,
        TOO_MANY_EQUIPMENT_PROFILES,
        TOO_MANY_FAVOURITES,
        MISSING_REQUIRED_FIELD,
        INVALID_RECORD,
        EMPTY_NAME,
        NAME_TOO_LONG,
        NOTE_TOO_LONG,
        COORDINATE_OUT_OF_RANGE,
        DUPLICATE_SPOT_ID,
        DUPLICATE_EQUIPMENT_PROFILE_ID,
        /** The spot's `equipmentProfileId` is not among the document's `equipmentProfiles`. */
        UNKNOWN_EQUIPMENT_PROFILE,
        WRONG_HORIZON_SAMPLE_COUNT,
        HORIZON_SAMPLE_OUT_OF_RANGE,
        UNSUPPORTED_HORIZON_ENCODING,
    }

    /**
     * A typed refusal. It carries **no user-facing prose** — [diagnostic] is a technical string for a log
     * or a bug report, and the screen chooses the sentence from [code], [field] and [recordIndex].
     */
    data class SpotTransferError(
        val code: SpotTransferErrorCode,
        val field: String? = null,
        val recordIndex: Int? = null,
        /** Best-effort 1-based line of the parse failure, when the decoder reported an offset. */
        val line: Int? = null,
        val diagnostic: String = "",
    )

    /** The decoded content of a scout document. */
    data class ScoutBundle(
        val spots: List<Scout>,
        val horizons: Map<String, HorizonProfile>,
        val favourites: Set<String>,
    )

    /** The result of decoding a document: the content, or a typed refusal. */
    sealed interface DecodeResult {
        data class Ok(val bundle: ScoutBundle) : DecodeResult
        data class Refused(val error: SpotTransferError) : DecodeResult
    }

    /**
     * A validated import plan, computed without touching the store, so the screen can show the counts
     * **before** the commit (`specs/scout-spots` *"with the count of records added and skipped shown
     * before committing"*). [Ready] carries exactly the records the commit will write.
     */
    sealed interface ImportPlan {
        data class Ready(
            val mode: ImportMode,
            val added: Int,
            val skipped: Int,
            val favouritesAdded: Int,
            /** The spots to add (merge) or to install (replace all). */
            val spots: List<Scout>,
            /** The horizon profiles those spots reference, keyed by profile id. */
            val horizons: Map<String, HorizonProfile>,
            /** The device's favourites after the import: the union, never a removal. */
            val favourites: Set<String>,
        ) : ImportPlan

        data class Refused(val error: SpotTransferError) : ImportPlan
    }

    /** The outcome of a committed import. */
    sealed interface ImportResult {
        data class Success(
            val mode: ImportMode,
            val added: Int,
            val skipped: Int,
            val favouritesAdded: Int,
            val activeSpotId: String?,
        ) : ImportResult

        data class Refused(val error: SpotTransferError) : ImportResult
    }

    // ---------------------------------------------------------------------------------------------
    // Document name and encoding
    // ---------------------------------------------------------------------------------------------

    /** `lumintra-scout-<yyyyMMdd>.json` in UTC, the name the export requires. */
    fun exportFileName(nowUtc: Instant): String =
        EXPORT_FILE_PREFIX + FILE_DATE_FORMAT.format(nowUtc) + ".json"

    /**
     * Serialises the whole scout setup into the document of `specs/scout-spots` *"The user's spots,
     * horizons and equipment export to a file they own"*.
     *
     * [spots] is written in the store's own list order (alphabetical by name, then by id), the equipment
     * profile list in first-reference order and the favourites sorted, so two exports of one state are
     * byte-identical and a diff of two exports is a diff of the plan.
     *
     * **Two things the spec's field list has no key for, stated rather than hidden.** A spot's
     * `approximate` flag and an equipment profile's `presetName` are stored on the device (see
     * [SpotRecordDto]) but are *not* in the document's field list, so an exported approximate spot
     * returns as an exact one and a preset-derived profile returns as a custom profile with the same
     * numbers and name. The numbers, the name and every coordinate are lossless; those two labels are
     * not, and the field list is the spec's to change, not this codec's to widen.
     */
    fun encodeDocument(
        spots: List<Scout>,
        horizons: Map<String, HorizonProfile>,
        favourites: Set<String>,
        nowUtc: Instant,
    ): String {
        val orderedSpots = sortSpots(spots)
        val equipment = exportedEquipment(orderedSpots)

        val document = DocumentDto(
            schemaVersion = SUPPORTED_SCHEMA_VERSION,
            app = APP_TAG,
            exportedAtUtc = nowUtc.truncatedTo(ChronoUnit.SECONDS).toString(),
            spots = orderedSpots.map { spot ->
                val horizon = spot.horizonProfileId?.let { horizons[it] }
                DocumentSpotDto(
                    id = spot.id,
                    name = spot.name,
                    latDeg = spot.latDeg,
                    lonDeg = spot.lonDeg,
                    elevationM = spot.elevationM,
                    tzId = spot.tzId,
                    // `Scout` has no `note`, and `design.md` fixes the emission order of the fields;
                    // the spec allows null in this position.
                    note = null,
                    equipmentProfileId = equipment.idsByProfile[equipmentKey(spot.equipment)],
                    horizonProfile = horizon?.let {
                        DocumentHorizonDto(samples = it.encodedSamples())
                    },
                )
            },
            equipmentProfiles = equipment.profiles,
            favouriteTargetIds = favourites.asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .sorted()
                .toList(),
        )
        return json.encodeToString(document)
    }

    // ---------------------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes a document. **Total** — it returns a typed refusal rather than throwing on any input,
     * including an empty string, a JSON array, a truncated file or a `schemaVersion` newer than this
     * build.
     *
     * Validation order is the order of the spec's own scrutiny, so the refusal names the first thing a
     * human would look at: shape and version, then the caps, then the equipment list, then each spot's
     * fields, then its horizon — and every refusal names the field and the record index.
     */
    fun decodeDocument(document: String): DecodeResult {
        if (document.isBlank()) {
            return DecodeResult.Refused(error(SpotTransferErrorCode.EMPTY_DOCUMENT))
        }
        if (document.length > MAX_DOCUMENT_CHARS) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.TOO_LARGE,
                    field = "document",
                    diagnostic = "length=${document.length} cap=$MAX_DOCUMENT_CHARS",
                ),
            )
        }

        // The header alone first: a version we do not know must be refused *before* the body is trusted,
        // and before any per-record work happens on a document we are going to reject.
        val header = try {
            json.decodeFromString<HeaderDto>(document)
        } catch (failure: Exception) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.NOT_A_SCOUT_FILE,
                    line = lineOf(document, failure),
                    diagnostic = diagnosticOf(failure),
                ),
            )
        }
        if (header.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.UNKNOWN_SCHEMA_VERSION,
                    field = "schemaVersion",
                    diagnostic = "found=${header.schemaVersion} supported=$SUPPORTED_SCHEMA_VERSION",
                ),
            )
        }
        if (header.schemaVersion < MIN_SCHEMA_VERSION) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.NOT_A_SCOUT_FILE,
                    field = "schemaVersion",
                    diagnostic = "found=${header.schemaVersion}",
                ),
            )
        }

        val root = try {
            json.decodeFromString<DocumentDto>(document)
        } catch (failure: Exception) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.NOT_A_SCOUT_FILE,
                    line = lineOf(document, failure),
                    diagnostic = diagnosticOf(failure),
                ),
            )
        }
        if (root.app != APP_TAG) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.NOT_A_SCOUT_FILE,
                    field = "app",
                    diagnostic = "app=${root.app}",
                ),
            )
        }
        if (root.spots.size > MAX_SPOTS) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.TOO_MANY_SPOTS,
                    field = "spots",
                    diagnostic = "count=${root.spots.size} cap=$MAX_SPOTS",
                ),
            )
        }
        if (root.equipmentProfiles.size > MAX_EQUIPMENT_PROFILES) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.TOO_MANY_EQUIPMENT_PROFILES,
                    field = "equipmentProfiles",
                    diagnostic = "count=${root.equipmentProfiles.size} cap=$MAX_EQUIPMENT_PROFILES",
                ),
            )
        }
        if (root.favouriteTargetIds.size > MAX_FAVOURITES) {
            return DecodeResult.Refused(
                error(
                    SpotTransferErrorCode.TOO_MANY_FAVOURITES,
                    field = "favouriteTargetIds",
                    diagnostic = "count=${root.favouriteTargetIds.size} cap=$MAX_FAVOURITES",
                ),
            )
        }

        val equipmentById = LinkedHashMap<String, EquipmentProfile>()
        root.equipmentProfiles.forEachIndexed { index, dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || id.length > MAX_ID_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "equipmentProfiles.id",
                        index,
                        diagnostic = "length=${id.length}",
                    ),
                )
            }
            if (equipmentById.containsKey(id)) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.DUPLICATE_EQUIPMENT_PROFILE_ID,
                        "equipmentProfiles.id",
                        index,
                        diagnostic = "id=$id",
                    ),
                )
            }
            if (!dto.focalLengthMm.isFinite() || dto.focalLengthMm <= 0.0 ||
                !dto.sensorWidthMm.isFinite() || dto.sensorWidthMm <= 0.0 ||
                !dto.sensorHeightMm.isFinite() || dto.sensorHeightMm <= 0.0
            ) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "equipmentProfiles",
                        index,
                        diagnostic = "f=${dto.focalLengthMm} w=${dto.sensorWidthMm} h=${dto.sensorHeightMm}",
                    ),
                )
            }
            val name = dto.name.trim().ifEmpty { id }
            if (name.length > MAX_NAME_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.NAME_TOO_LONG,
                        "equipmentProfiles.name",
                        index,
                        diagnostic = "length=${name.length} cap=$MAX_NAME_LENGTH",
                    ),
                )
            }
            equipmentById[id] = EquipmentProfile(
                name = name,
                focalLengthMm = dto.focalLengthMm,
                sensorWidthMm = dto.sensorWidthMm,
                sensorHeightMm = dto.sensorHeightMm,
                presetName = null,
            )
        }

        val spots = ArrayList<Scout>(root.spots.size)
        val horizons = LinkedHashMap<String, HorizonProfile>()
        val seenIds = HashSet<String>()

        root.spots.forEachIndexed { index, dto ->
            val name = dto.name.trim()
            if (name.isEmpty()) {
                return DecodeResult.Refused(error(SpotTransferErrorCode.EMPTY_NAME, "name", index))
            }
            if (name.length > MAX_NAME_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.NAME_TOO_LONG,
                        "name",
                        index,
                        diagnostic = "length=${name.length} cap=$MAX_NAME_LENGTH",
                    ),
                )
            }
            if (dto.id.isBlank()) {
                return DecodeResult.Refused(
                    error(SpotTransferErrorCode.MISSING_REQUIRED_FIELD, "id", index),
                )
            }
            if (dto.id.length > MAX_ID_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "id",
                        index,
                        diagnostic = "length=${dto.id.length} cap=$MAX_ID_LENGTH",
                    ),
                )
            }
            if (!seenIds.add(dto.id)) {
                return DecodeResult.Refused(
                    error(SpotTransferErrorCode.DUPLICATE_SPOT_ID, "id", index, diagnostic = "id=${dto.id}"),
                )
            }
            // Latitude is the CLOSED range [-90, +90]: a pole is a legal spot (specs/scout-spots), and a
            // value outside it refuses the whole document rather than being clamped or wrapped.
            if (!dto.latDeg.isFinite() || dto.latDeg !in -90.0..90.0) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE,
                        "latDeg",
                        index,
                        diagnostic = "latDeg=${dto.latDeg}",
                    ),
                )
            }
            if (!dto.lonDeg.isFinite() || dto.lonDeg !in -180.0..180.0) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.COORDINATE_OUT_OF_RANGE,
                        "lonDeg",
                        index,
                        diagnostic = "lonDeg=${dto.lonDeg}",
                    ),
                )
            }
            if (!dto.elevationM.isFinite()) {
                return DecodeResult.Refused(
                    error(SpotTransferErrorCode.INVALID_RECORD, "elevationM", index),
                )
            }
            val tzId = dto.tzId.trim()
            if (tzId.isEmpty() || tzId.length > MAX_TZ_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "tzId",
                        index,
                        diagnostic = "length=${tzId.length}",
                    ),
                )
            }
            val note = dto.note
            if (note != null && note.length > MAX_NOTE_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.NOTE_TOO_LONG,
                        "note",
                        index,
                        diagnostic = "length=${note.length} cap=$MAX_NOTE_LENGTH",
                    ),
                )
            }

            // `equipmentProfileId` is a reference INTO this document, so an id that is not there is an
            // invalid record rather than a reason to fall back to a default lens: silently substituting a
            // lens would change every field of view the spot's brief is computed from. A null id is the
            // legal "no equipment recorded" state and takes the domain default.
            val equipmentId = dto.equipmentProfileId?.trim()?.takeIf { it.isNotEmpty() }
            val equipment = if (equipmentId == null) {
                EquipmentProfile.DEFAULT
            } else {
                equipmentById[equipmentId] ?: return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.UNKNOWN_EQUIPMENT_PROFILE,
                        "equipmentProfileId",
                        index,
                        diagnostic = "id=$equipmentId",
                    ),
                )
            }

            var horizon: HorizonProfile? = null
            var horizonProfileId: String? = null
            val horizonDto = dto.horizonProfile
            if (horizonDto != null) {
                if (horizonDto.sampleCount != HorizonProfile.SAMPLE_COUNT) {
                    return DecodeResult.Refused(
                        error(
                            SpotTransferErrorCode.WRONG_HORIZON_SAMPLE_COUNT,
                            "horizonProfile.sampleCount",
                            index,
                            diagnostic = "sampleCount=${horizonDto.sampleCount}",
                        ),
                    )
                }
                if (horizonDto.samples.size != HorizonProfile.SAMPLE_COUNT) {
                    return DecodeResult.Refused(
                        error(
                            SpotTransferErrorCode.WRONG_HORIZON_SAMPLE_COUNT,
                            "horizonProfile.samples",
                            index,
                            diagnostic = "samples=${horizonDto.samples.size}",
                        ),
                    )
                }
                if (horizonDto.azimuthStepDeg != HORIZON_AZIMUTH_STEP_DEG ||
                    horizonDto.elevationUnitDeg != HORIZON_ELEVATION_UNIT_DEG
                ) {
                    return DecodeResult.Refused(
                        error(
                            SpotTransferErrorCode.UNSUPPORTED_HORIZON_ENCODING,
                            "horizonProfile",
                            index,
                            diagnostic = "azimuthStepDeg=${horizonDto.azimuthStepDeg} " +
                                "elevationUnitDeg=${horizonDto.elevationUnitDeg}",
                        ),
                    )
                }
                val outOfRange = horizonDto.samples.firstOrNull {
                    it < HorizonProfile.MIN_STORED || it > HorizonProfile.MAX_STORED
                }
                if (outOfRange != null) {
                    return DecodeResult.Refused(
                        error(
                            SpotTransferErrorCode.HORIZON_SAMPLE_OUT_OF_RANGE,
                            "horizonProfile.samples",
                            index,
                            diagnostic = "value=$outOfRange",
                        ),
                    )
                }
                // The document carries no horizon id, so the profile's id is derived from the spot it
                // belongs to — deterministic, so re-importing the same file yields the same record.
                val derivedId = horizonIdFor(dto.id)
                val profile = profileFromSamples(
                    id = derivedId,
                    name = name,
                    latitudeDeg = dto.latDeg,
                    longitudeDeg = dto.lonDeg,
                    samples = horizonDto.samples,
                ) ?: return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.WRONG_HORIZON_SAMPLE_COUNT,
                        "horizonProfile.samples",
                        index,
                    ),
                )
                horizonProfileId = derivedId
                horizon = profile
            }

            val spot = try {
                Scout(
                    id = dto.id,
                    name = name,
                    latDeg = dto.latDeg,
                    lonDeg = dto.lonDeg,
                    elevationM = dto.elevationM,
                    tzId = tzId,
                    horizonProfileId = horizonProfileId,
                    equipment = equipment,
                    approximate = false,
                )
            } catch (failure: Exception) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "spot",
                        index,
                        diagnostic = diagnosticOf(failure),
                    ),
                )
            }

            spots.add(spot)
            if (horizon != null) horizons[horizon.id] = horizon
        }

        val favourites = LinkedHashSet<String>()
        root.favouriteTargetIds.forEachIndexed { index, raw ->
            val id = raw.trim()
            if (id.isEmpty()) return@forEachIndexed
            if (id.length > MAX_ID_LENGTH) {
                return DecodeResult.Refused(
                    error(
                        SpotTransferErrorCode.INVALID_RECORD,
                        "favouriteTargetIds",
                        index,
                        diagnostic = "length=${id.length} cap=$MAX_ID_LENGTH",
                    ),
                )
            }
            favourites.add(id)
        }

        return DecodeResult.Ok(
            ScoutBundle(spots = spots, horizons = horizons, favourites = favourites),
        )
    }

    /**
     * Builds the import plan **without writing anything**, so the screen can show the added / skipped /
     * favourite counts before the commit. [plan] and [ScoutStore.importJson] share this one
     * implementation, so the preview cannot disagree with the commit.
     *
     * A failed validation refuses the whole document, so a partial import is impossible
     * (all-or-nothing).
     */
    fun plan(
        document: String,
        existingSpots: List<Scout>,
        existingFavourites: Set<String>,
        mode: ImportMode,
    ): ImportPlan {
        val bundle = when (val decoded = decodeDocument(document)) {
            is DecodeResult.Ok -> decoded.bundle
            is DecodeResult.Refused -> return ImportPlan.Refused(decoded.error)
        }

        // Favourites always merge as a union of stable ids: an import never removes a favourite the user
        // already had, and never duplicates an id.
        val favouritesFinal = existingFavourites + bundle.favourites
        val favouritesAdded = favouritesFinal.size - existingFavourites.size

        return when (mode) {
            ImportMode.REPLACE_ALL -> ImportPlan.Ready(
                mode = mode,
                added = bundle.spots.size,
                skipped = 0,
                favouritesAdded = favouritesAdded,
                spots = bundle.spots,
                horizons = bundle.horizons,
                favourites = favouritesFinal,
            )

            ImportMode.MERGE -> {
                // `Scout.mergeKey()` is the one definition of "the same spot": name and coordinates
                // exactly equal, compared at the stored precision.
                val seenKeys = existingSpots.mapTo(HashSet()) { it.mergeKey() }
                val added = ArrayList<Scout>()
                val horizons = LinkedHashMap<String, HorizonProfile>()
                var skipped = 0
                for (spot in bundle.spots) {
                    if (!seenKeys.add(spot.mergeKey())) {
                        skipped++
                        continue
                    }
                    added.add(spot)
                    spot.horizonProfileId?.let { id -> bundle.horizons[id]?.let { horizons[it.id] = it } }
                }
                ImportPlan.Ready(
                    mode = mode,
                    added = added.size,
                    skipped = skipped,
                    favouritesAdded = favouritesAdded,
                    spots = added,
                    horizons = horizons,
                    favourites = favouritesFinal,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Per-record store codec ([ScoutStore] keeps one JSON string per spot and per horizon)
    // ---------------------------------------------------------------------------------------------

    /**
     * Serialises one spot for the on-device store.
     *
     * The store record is **wider** than the export document on purpose: it keeps `approximate`,
     * `presetName` and the spot's own `horizonProfileId`, because on the device they are facts about the
     * user's plan that no other field can reconstruct. It never leaves the device, so it is not bound by
     * the document's field list.
     */
    fun encodeSpotRecord(spot: Scout): String = json.encodeToString(
        SpotRecordDto(
            id = spot.id,
            name = spot.name,
            latDeg = spot.latDeg,
            lonDeg = spot.lonDeg,
            elevationM = spot.elevationM,
            tzId = spot.tzId,
            approximate = spot.approximate,
            horizonProfileId = spot.horizonProfileId,
            equipment = spot.equipment.toEquipmentRecord(),
        ),
    )

    /**
     * Parses one spot record; `null` means that single record is unreadable and must be quarantined —
     * which is also the answer for a record written by a **newer** build, so a version this build does
     * not understand can never be decoded under this build's field meanings.
     */
    fun decodeSpotRecord(text: String): Scout? = try {
        val dto = json.decodeFromString<SpotRecordDto>(text)
        if (dto.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            null
        } else {
            Scout(
                id = dto.id,
                name = dto.name,
                latDeg = dto.latDeg,
                lonDeg = dto.lonDeg,
                elevationM = dto.elevationM,
                tzId = dto.tzId,
                horizonProfileId = dto.horizonProfileId,
                equipment = dto.equipment.toDomain(),
                approximate = dto.approximate,
            )
        }
    } catch (failure: Exception) {
        null
    }

    /** Serialises one horizon profile for the store, including its id and the place it was drawn at. */
    fun encodeHorizonRecord(profile: HorizonProfile): String = json.encodeToString(
        HorizonRecordDto(
            id = profile.id,
            name = profile.name,
            latitudeDeg = profile.latitudeDeg,
            longitudeDeg = profile.longitudeDeg,
            samples = profile.encodedSamples(),
        ),
    )

    /**
     * Parses one horizon record; `null` means that single record is unreadable and must be quarantined —
     * including a record written by a newer build, for the same reason as [decodeSpotRecord].
     */
    fun decodeHorizonRecord(text: String): HorizonProfile? = try {
        val dto = json.decodeFromString<HorizonRecordDto>(text)
        if (dto.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            null
        } else if (dto.sampleCount != HorizonProfile.SAMPLE_COUNT) {
            null
        } else if (dto.azimuthStepDeg != HORIZON_AZIMUTH_STEP_DEG) {
            null
        } else if (dto.elevationUnitDeg != HORIZON_ELEVATION_UNIT_DEG) {
            null
        } else if (dto.samples.size != HorizonProfile.SAMPLE_COUNT) {
            null
        } else if (dto.samples.any { it < HorizonProfile.MIN_STORED || it > HorizonProfile.MAX_STORED }) {
            null
        } else {
            profileFromSamples(
                id = dto.id,
                name = dto.name,
                latitudeDeg = dto.latitudeDeg,
                longitudeDeg = dto.longitudeDeg,
                samples = dto.samples,
            )
        }
    } catch (failure: Exception) {
        null
    }

    /** A stable string set, sorted, so the store's favourites entry is deterministic. */
    fun encodeFavouriteIds(ids: Set<String>): String = json.encodeToString(
        ids.asSequence().map { it.trim() }.filter { it.isNotEmpty() }.sorted().toList(),
    )

    /** Parses the favourites entry; a corrupt entry reads as the empty set rather than throwing. */
    fun decodeFavouriteIds(text: String): Set<String> = try {
        json.decodeFromString<List<String>>(text)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    } catch (failure: Exception) {
        emptySet()
    }

    // ---------------------------------------------------------------------------------------------
    // DTOs (private: the JSON shape is an implementation detail of this codec)
    // ---------------------------------------------------------------------------------------------

    /** Read first, so a newer `schemaVersion` is refused before the rest of the document is trusted. */
    @Serializable
    private data class HeaderDto(val schemaVersion: Int)

    /**
     * The document, in the spec's field order — `schemaVersion` first, then the writer, then the
     * envelope, then spots, equipment and favourites. `kotlinx.serialization` preserves declaration
     * order, so the declaration *is* the file's key order.
     */
    @Serializable
    private data class DocumentDto(
        val schemaVersion: Int,
        val app: String,
        val exportedAtUtc: String = "",
        val spots: List<DocumentSpotDto> = emptyList(),
        val equipmentProfiles: List<DocumentEquipmentDto> = emptyList(),
        val favouriteTargetIds: List<String> = emptyList(),
    )

    /** A spot exactly as the document's field list defines it — and no key beyond it. */
    @Serializable
    private data class DocumentSpotDto(
        val id: String,
        val name: String,
        val latDeg: Double,
        val lonDeg: Double,
        val elevationM: Double = 0.0,
        val tzId: String,
        val note: String? = null,
        val equipmentProfileId: String? = null,
        val horizonProfile: DocumentHorizonDto? = null,
    )

    /** A horizon inside a document: exactly four keys, the sample array as tenths of a degree. */
    @Serializable
    private data class DocumentHorizonDto(
        val sampleCount: Int = HorizonProfile.SAMPLE_COUNT,
        val azimuthStepDeg: Double = HORIZON_AZIMUTH_STEP_DEG,
        val elevationUnitDeg: Double = HORIZON_ELEVATION_UNIT_DEG,
        val samples: List<Int> = emptyList(),
    )

    /** An equipment profile inside a document: exactly five keys, all in millimetres. */
    @Serializable
    private data class DocumentEquipmentDto(
        val id: String,
        val name: String,
        val focalLengthMm: Double,
        val sensorWidthMm: Double,
        val sensorHeightMm: Double,
    )

    /**
     * The on-device spot record. It is the store's own shape rather than the document's, so the store
     * keeps `approximate` and the equipment's `presetName` — both of which the export document's field
     * list has no key for.
     *
     * `schemaVersion` comes first, exactly as in the export document, because the requirement is that
     * the store persists in a **versioned** on-device format: a record written by a newer build must be
     * refused (quarantined) rather than decoded under this build's field meanings. It carries a default
     * so a record written before the field existed still reads as version 1.
     */
    @Serializable
    private data class SpotRecordDto(
        val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,
        val id: String,
        val name: String,
        val latDeg: Double,
        val lonDeg: Double,
        val elevationM: Double = 0.0,
        val tzId: String,
        val approximate: Boolean = false,
        val horizonProfileId: String? = null,
        val equipment: EquipmentRecordDto = EquipmentRecordDto(),
    )

    /** The on-device horizon record: the document's four keys plus the profile's own identity. */
    @Serializable
    private data class HorizonRecordDto(
        val schemaVersion: Int = SUPPORTED_SCHEMA_VERSION,
        val id: String,
        val name: String,
        val latitudeDeg: Double,
        val longitudeDeg: Double,
        val sampleCount: Int = HorizonProfile.SAMPLE_COUNT,
        val azimuthStepDeg: Double = HORIZON_AZIMUTH_STEP_DEG,
        val elevationUnitDeg: Double = HORIZON_ELEVATION_UNIT_DEG,
        val samples: List<Int> = emptyList(),
    )

    @Serializable
    private data class EquipmentRecordDto(
        val name: String = EquipmentProfile.DEFAULT.name,
        val focalLengthMm: Double = EquipmentProfile.DEFAULT.focalLengthMm,
        val sensorWidthMm: Double = EquipmentProfile.DEFAULT.sensorWidthMm,
        val sensorHeightMm: Double = EquipmentProfile.DEFAULT.sensorHeightMm,
        val presetName: String? = null,
    )

    // ---------------------------------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------------------------------

    private fun sortSpots(spots: List<Scout>): List<Scout> =
        spots.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Scout::name).thenBy(Scout::id))

    /** The equipment entries the document carries, plus the id each exported spot references. */
    private class ExportedEquipment(
        val profiles: List<DocumentEquipmentDto>,
        val idsByProfile: Map<String, String>,
    )

    /** Two profiles are the same exported row when their name and their three numbers agree. */
    private fun equipmentKey(profile: EquipmentProfile): String =
        profile.name + "|" + profile.focalLengthMm + "|" + profile.sensorWidthMm + "|" +
            profile.sensorHeightMm

    /**
     * De-duplicates the spots' equipment into the document's `equipmentProfiles` list, in
     * first-reference order, and remembers which id each distinct profile received.
     *
     * A profile's id is derived from its millimetres — `eq-<f>-<w>-<h>`, with a numeric suffix only when
     * two *differently named* profiles share those numbers — so it is stable across exports and needs no
     * second identifier in the domain model. `Double.toString()` is used rather than any locale-aware
     * formatting, so the id is byte-identical on a device set to `ru`.
     */
    private fun exportedEquipment(orderedSpots: List<Scout>): ExportedEquipment {
        val profiles = ArrayList<DocumentEquipmentDto>()
        val idsByKey = LinkedHashMap<String, String>()

        for (spot in orderedSpots) {
            val profile = spot.equipment
            val key = equipmentKey(profile)
            if (idsByKey.containsKey(key)) continue

            val stem = "eq-" + profile.focalLengthMm + "-" + profile.sensorWidthMm + "-" +
                profile.sensorHeightMm
            var id = stem
            var suffix = 2
            while (profiles.any { it.id == id }) {
                id = "$stem-$suffix"
                suffix++
            }
            profiles.add(
                DocumentEquipmentDto(
                    id = id,
                    name = profile.name,
                    focalLengthMm = profile.focalLengthMm,
                    sensorWidthMm = profile.sensorWidthMm,
                    sensorHeightMm = profile.sensorHeightMm,
                ),
            )
            idsByKey[key] = id
        }
        return ExportedEquipment(profiles = profiles, idsByProfile = idsByKey)
    }

    private fun EquipmentProfile.toEquipmentRecord(): EquipmentRecordDto = EquipmentRecordDto(
        name = name,
        focalLengthMm = focalLengthMm,
        sensorWidthMm = sensorWidthMm,
        sensorHeightMm = sensorHeightMm,
        presetName = presetName,
    )

    private fun EquipmentRecordDto.toDomain(): EquipmentProfile = EquipmentProfile(
        name = name,
        focalLengthMm = focalLengthMm,
        sensorWidthMm = sensorWidthMm,
        sensorHeightMm = sensorHeightMm,
        presetName = presetName,
    )

    /** The id a document's spot gives its horizon profile; deterministic and derived, never stored. */
    private fun horizonIdFor(spotId: String): String = "horizon-$spotId"

    /**
     * The 360 tenths-of-a-degree integers, read back **through [HorizonProfile.encode]** so the rounding
     * and the clamp have exactly one implementation (the domain's own codec).
     */
    private fun HorizonProfile.encodedSamples(): List<Int> {
        val bytes = encode()
        val out = ArrayList<Int>(HorizonProfile.SAMPLE_COUNT)
        for (index in 0 until HorizonProfile.SAMPLE_COUNT) {
            val low = bytes[index * 2].toInt() and 0xFF
            val high = bytes[index * 2 + 1].toInt() and 0xFF
            val raw = (high shl 8) or low
            out.add(if (raw and 0x8000 != 0) raw - 0x10000 else raw)
        }
        return out
    }

    /** The JSON integer array back to a [HorizonProfile]**through [HorizonProfile.decode]**. */
    private fun profileFromSamples(
        id: String,
        name: String,
        latitudeDeg: Double,
        longitudeDeg: Double,
        samples: List<Int>,
    ): HorizonProfile? {
        if (samples.size != HorizonProfile.SAMPLE_COUNT) return null
        if (samples.any { it < HorizonProfile.MIN_STORED || it > HorizonProfile.MAX_STORED }) return null
        val bytes = ByteArray(HorizonProfile.ENCODED_BYTES)
        for (index in 0 until HorizonProfile.SAMPLE_COUNT) {
            val raw = samples[index]
            bytes[index * 2] = (raw and 0xFF).toByte()
            bytes[index * 2 + 1] = ((raw shr 8) and 0xFF).toByte()
        }
        return HorizonProfile.decode(id, name, latitudeDeg, longitudeDeg, bytes)
    }

    private fun error(
        code: SpotTransferErrorCode,
        field: String? = null,
        recordIndex: Int? = null,
        line: Int? = null,
        diagnostic: String = "",
    ): SpotTransferError = SpotTransferError(code, field, recordIndex, line, diagnostic)

    private fun diagnosticOf(failure: Throwable): String {
        val raw = failure.message ?: return failure.javaClass.simpleName
        return raw.lineSequence().firstOrNull()?.take(MAX_DIAGNOSTIC_LENGTH)
            ?: failure.javaClass.simpleName
    }

    /**
     * A best-effort 1-based line for a parse failure. `kotlinx.serialization` reports a character
     * **offset**, not a line, so the line is counted from the offset; a decoder that reports no offset
     * yields `null` rather than a guessed line.
     */
    private fun lineOf(document: String, failure: Throwable): Int? {
        val message = failure.message ?: return null
        val match = OFFSET_PATTERN.find(message) ?: return null
        val offset = match.groupValues[1].toIntOrNull() ?: return null
        if (offset <= 0) return 1
        val end = minOf(offset, document.length)
        var line = 1
        for (index in 0 until end) {
            if (document[index] == '\n') line++
        }
        return line
    }

    private val OFFSET_PATTERN = Regex("""offset\s+(\d+)""")
}

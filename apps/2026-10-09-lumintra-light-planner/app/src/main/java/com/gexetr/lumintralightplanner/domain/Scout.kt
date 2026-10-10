package com.gexetr.lumintralightplanner.domain

/**
 * A saved observing spot: where the user stands, and which zone its clock is in.
 *
 * [tzId] is an **IANA zone id** (`"Europe/Oslo"`), never a numeric offset, so DST is resolved by the
 * platform's tz database. A stored offset would be wrong for half the year.
 */
data class Scout(
    val id: String,
    val name: String,
    val latDeg: Double,
    val lonDeg: Double,
    val elevationM: Double = 0.0,
    val tzId: String,
    /** The saved horizon profile for this spot, or `null` when none has been drawn. */
    val horizonProfileId: String? = null,
    val equipment: EquipmentProfile = EquipmentProfile.DEFAULT,
    /**
     * True when the coordinates came from a place-name lookup or a pasted value rather than a precise
     * entry, so the UI labels the spot `approximate`.
     */
    val approximate: Boolean = false,
) {
    init {
        require(latDeg in -90.0..90.0) { "latDeg $latDeg outside [-90, 90]" }
        require(lonDeg in -180.0..180.0) { "lonDeg $lonDeg outside [-180, 180]" }
        require(tzId.isNotBlank()) { "a spot must carry an IANA zone id" }
    }

    val point: GeodeticPoint get() = GeodeticPoint(latDeg, lonDeg, elevationM)

    /** The coordinate the network clients may send: truncated to 4 decimals. */
    val networkPoint: GeodeticPoint get() = point.networkRounded()

    /**
     * The identity a `merge` import de-duplicates on: **name and coordinates exactly equal**.
     *
     * Coordinates are compared at the stored precision rather than by object identity, so the same spot
     * exported and re-imported is recognised.
     */
    fun mergeKey(): String = "$name|$latDeg|$lonDeg"
}

/**
 * The resolving rule for the active spot, which must survive a cold start, a deleted spot and an
 * unrelated import.
 *
 * The rule is stated as code rather than as a comment because every branch is a case the user hits: an
 * empty store has no active spot, a dangling id falls back to a real saved spot, and the fallback is
 * **deterministic** (the store's first record) so the app cannot appear to pick a random spot.
 */
object ScoutSelection {

    /**
     * Resolves [activeSpotId] against the store's records.
     *
     * Returns `null` only when there is nothing to select. A `null`, unknown or quarantined id falls
     * back to the first saved spot rather than leaving the app with a fabricated `(0, 0)` sky.
     */
    fun resolve(spots: List<Scout>, activeSpotId: String?): Scout? {
        if (spots.isEmpty()) return null
        if (activeSpotId != null) {
            spots.firstOrNull { it.id == activeSpotId }?.let { return it }
        }
        return spots.first()
    }

    /**
     * The id to persist after [spots] changed — a delete or an import. It is the current selection when
     * it still resolves, otherwise the fallback's id, otherwise `null` for an empty store.
     */
    fun reconcile(spots: List<Scout>, activeSpotId: String?): String? =
        resolve(spots, activeSpotId)?.id

    /** True when [activeSpotId] names nothing in [spots] — the dangling case the UI repairs. */
    fun isDangling(spots: List<Scout>, activeSpotId: String?): Boolean =
        activeSpotId != null && spots.isNotEmpty() && spots.none { it.id == activeSpotId }
}

/**
 * The equipment a spot is used with: the effective focal length and sensor the app computes every
 * field-of-view number from, all in **millimetres**.
 *
 * [presetName] records which named preset the values came from when the user chose one, so a custom
 * entry is distinguishable from a preset that happens to match.
 */
data class EquipmentProfile(
    val name: String,
    val focalLengthMm: Double,
    val sensorWidthMm: Double,
    val sensorHeightMm: Double,
    val presetName: String? = null,
) {
    init {
        require(focalLengthMm > 0.0) { "focal length must be positive" }
        require(sensorWidthMm > 0.0 && sensorHeightMm > 0.0) { "sensor extents must be positive" }
    }

    /** The lens form, so the fit statement has exactly one type to compute from. */
    fun toLensPreset(pixelPitchUm: Double? = null): LensPreset = LensPreset(
        name = presetName ?: name,
        sensorWidthMm = sensorWidthMm,
        sensorHeightMm = sensorHeightMm,
        focalLengthMm = focalLengthMm,
        pixelPitchUm = pixelPitchUm,
    )

    companion object {
        /**
         * The default lens, and the **first declared preset** (settings row P7 makes the default the
         * first entry, so the two cannot drift).
         *
         * 24 mm on a full-frame 36×24 mm sensor: `2·atan(36/48) = 73.7°` horizontal and
         * `2·atan(24/48) = 53.1°` vertical — the 73° × 53° the scene capability names, and therefore a
         * canvas of 146° × 106° with its 50 % context margin.
         */
        val DEFAULT = EquipmentProfile(
            name = "24 mm full frame",
            focalLengthMm = 24.0,
            sensorWidthMm = 36.0,
            sensorHeightMm = 24.0,
        )

        /** The presets the editor offers, each a real combination rather than a round number. */
        val PRESETS: List<EquipmentProfile> = listOf(
            DEFAULT,
            EquipmentProfile("50 mm full frame", 50.0, 36.0, 24.0),
            EquipmentProfile("135 mm full frame", 135.0, 36.0, 24.0),
            EquipmentProfile("200 mm full frame", 200.0, 36.0, 24.0),
            EquipmentProfile("18 mm APS-C", 18.0, 23.6, 15.7),
            EquipmentProfile("50 mm APS-C", 50.0, 23.6, 15.7),
            EquipmentProfile("14 mm Micro 4/3", 14.0, 17.3, 13.0),
            EquipmentProfile("Smartphone main", 4.3, 5.6, 4.2),
        )
    }
}

package com.gexetr.lumintralightplanner.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.lumintralightplanner.domain.HorizonProfile
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Which kind of record a quarantine entry describes.
 *
 * An enum rather than a prose string, so the `Spots` screen names the row and chooses its own sentence
 * from a string resource; the store never renders user-facing text.
 */
enum class QuarantineKind { SPOT, HORIZON }

/**
 * One record that failed to load, kept so the `Spots` screen can show it as a quarantine row rather than
 * removing it (`specs/scout-spots` *"A quarantined spot is shown, not silently dropped"*, E98/E103).
 *
 * [id] is the stable key the record was stored under, with its `spot:`/`horizon:` prefix stripped —
 * never a rendered sentence. [preserved] is true when the record has been **moved into the quarantine
 * bucket** by a destructive write ([ScoutStore.replaceAll] or a `REPLACE_ALL` import) and therefore no
 * longer occupies its own key; it is false while the record is still in place and is merely unreadable.
 */
data class QuarantinedRecord(
    val id: String,
    val kind: QuarantineKind,
    val preserved: Boolean,
)

/**
 * Everything the scout store holds, as one immutable value.
 *
 * [spots] is already in the list order the `Spots` screen requires — **alphabetical by [Scout.name],
 * case-insensitively, then ascending by [Scout.id]** — so the order cannot depend on the order records
 * were written.
 *
 * [activeSpotId] is the **resolved** active spot's id, not the raw persisted value: when the persisted
 * id is dangling (or absent), `ScoutSelection.resolve` falls back to the first non-quarantined spot, and
 * that is what is reported here. [quarantinedCount] counts records that failed to load and were set
 * aside, and [quarantined] names them; [missingTargetRefCount] counts favourite ids that a catalogue
 * reconciliation dropped.
 *
 * [quarantined] is an **addition** to the frozen interface of `INTERFACES.md`, not a replacement: it is
 * the last parameter and carries a default, so every existing `ScoutSnapshot(spots, horizons,
 * activeSpotId, favourites, quarantinedCount, missingTargetRefCount)` construction compiles unchanged.
 * It exists because a count alone cannot satisfy the spec's *"shown in the list as a quarantined row"*.
 */
data class ScoutSnapshot(
    val spots: List<Scout>,
    val horizons: Map<String, HorizonProfile>,
    val activeSpotId: String?,
    val favourites: Set<String>,
    val quarantinedCount: Int,
    val missingTargetRefCount: Int,
    val quarantined: List<QuarantinedRecord> = emptyList(),
)

/**
 * The one DataStore instance for the scout store.
 *
 * It is a separate file from `app_settings` (`SettingsStore.kt`) because the scout is the plan and the
 * settings are preferences: they are written by different flows and a corrupt one must not cost the
 * other. It is `private` to this file so the only route to the plan on disk is through [ScoutStore].
 */
private val Context.scoutDataStore by preferencesDataStore(name = "lumintra_scout")

private val ACTIVE_SPOT_KEY = stringPreferencesKey("active_spot_id")
private val FAVOURITES_KEY = stringPreferencesKey("favourite_target_ids")
private val PRUNED_FAVOURITES_KEY = stringPreferencesKey("pruned_target_ids")

/** The prefix of the record keys: `spot:<id>` and `horizon:<id>`, one JSON string each. */
private const val SPOT_KEY_PREFIX: String = "spot:"
private const val HORIZON_KEY_PREFIX: String = "horizon:"

/**
 * The prefix of the quarantine bucket, `quarantine:<original record key>`.
 *
 * A record that fails to parse is left in place on read, so it survives an ordinary save. It is **moved
 * here** only by a write that would otherwise destroy it — [ScoutStore.replaceAll] and a `REPLACE_ALL`
 * import — which is what makes the quarantine durable (E103) instead of a silent discard on the next
 * save. Preferences DataStore has no free-form map, so the bucket is a key per preserved record and
 * never a second file.
 */
private const val QUARANTINE_KEY_PREFIX: String = "quarantine:"

private fun spotPreferencesKey(id: String) = stringPreferencesKey("$SPOT_KEY_PREFIX$id")

private fun horizonPreferencesKey(id: String) = stringPreferencesKey("$HORIZON_KEY_PREFIX$id")

/**
 * Persists the user's scout: saved spots (each carrying its equipment), their horizon profiles, the
 * **store-level `activeSpotId`** and the favourite target ids.
 *
 * **Storage architecture** (`design.md` § Persistence, `specs/scout-spots` "The scout store persists…"):
 *
 * - Preferences DataStore holds **one JSON string per record** — `spot:<id>` and `horizon:<id>` — written
 *   by [SpotTransfer]. A record is therefore isolated: a truncated `spot:<id>` costs that one spot and
 *   leaves every other intact. Each record is **versioned** (`schemaVersion` first), so a record written
 *   by a newer build is quarantined rather than decoded under this build's field meanings.
 * - **Loading is per record.** [readSnapshot] parses each record on its own; a record that fails to parse
 *   is **quarantined** — counted in [ScoutSnapshot.quarantinedCount] and excluded from [ScoutSnapshot.spots]
 *   — and the other records load. A quarantined record is **not** deleted on read, so it survives the next
 *   save and stays visible to the user rather than being silently dropped.
 * - **Writes are atomic.** Every write is a single `DataStore.edit {}`; Preferences DataStore replaces its
 *   file atomically (temp file, then rename), so a crash mid-write leaves the previous good store and never
 *   a truncated one.
 * - **The active spot and the favourites are store-level values**, neither a field of a [Scout] nor a part
 *   of the export document, so a cold start reopens at the same spot and the importing device derives its
 *   own selection.
 *
 * The class deliberately exposes the interface the screens read ([ScoutSnapshot] plus the writes) and
 * **no** catalogue dependency: favourites are reconciled against catalogue ids by [pruneFavourites], which
 * the caller supplies.
 */
class ScoutStore(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * The store as a stream of values. An unreadable DataStore file degrades to an empty snapshot
     * (caught [IOException]) rather than throwing into the composition.
     */
    val snapshot: Flow<ScoutSnapshot> = appContext.scoutDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> readSnapshot(preferences) }

    /** Adds or replaces one spot. The first spot saved into an empty store becomes the active one. */
    suspend fun upsertSpot(spot: Scout) {
        appContext.scoutDataStore.edit { preferences ->
            preferences[spotPreferencesKey(spot.id)] = SpotTransfer.encodeSpotRecord(spot)
            if (preferences[ACTIVE_SPOT_KEY]?.takeIf { it.isNotBlank() } == null) {
                preferences[ACTIVE_SPOT_KEY] = spot.id
            }
        }
    }

    /**
     * Removes one spot. If it was the active spot, the first remaining non-quarantined spot in the list
     * order becomes active; deleting the last spot leaves no active spot.
     *
     * The spot's horizon record is **kept** so a delete-undo restores the spot intact; orphaned horizon
     * records are cleared by [replaceAll] and by a `REPLACE_ALL` import.
     */
    suspend fun deleteSpot(id: String) {
        appContext.scoutDataStore.edit { preferences ->
            preferences.remove(spotPreferencesKey(id))
            val remaining = sortSpots(readSpotRecords(preferences))
            val currentActive = preferences[ACTIVE_SPOT_KEY]?.takeIf { it.isNotBlank() }
            val next = ScoutSelection.reconcile(remaining, currentActive)
            if (next == null) preferences.remove(ACTIVE_SPOT_KEY) else preferences[ACTIVE_SPOT_KEY] = next
        }
    }

    /** Sets the store-level active spot, or clears it when [id] is null or blank. */
    suspend fun setActiveSpot(id: String?) {
        appContext.scoutDataStore.edit { preferences ->
            if (id.isNullOrBlank()) preferences.remove(ACTIVE_SPOT_KEY)
            else preferences[ACTIVE_SPOT_KEY] = id
        }
    }

    /** Adds or replaces one horizon profile, keyed by its stable id. */
    suspend fun upsertHorizon(profile: HorizonProfile) {
        appContext.scoutDataStore.edit { preferences ->
            preferences[horizonPreferencesKey(profile.id)] = SpotTransfer.encodeHorizonRecord(profile)
        }
    }

    /**
     * Replaces the favourite target set. Blank ids are dropped; the set is stored sorted so the entry is
     * deterministic. The ids are stable strings (`m31`, `ngc7000`), never list indices.
     */
    suspend fun setFavourites(ids: Set<String>) {
        appContext.scoutDataStore.edit { preferences ->
            preferences[FAVOURITES_KEY] = SpotTransfer.encodeFavouriteIds(ids)
            preferences.remove(PRUNED_FAVOURITES_KEY)
        }
    }

    /**
     * Replaces **every** saved spot and horizon in one atomic write, and sets the favourites union with
     * nothing existing (a full replace). The active spot is kept when it still resolves, otherwise the
     * first spot of the new set becomes active.
     */
    suspend fun replaceAll(
        spots: List<Scout>,
        horizons: List<HorizonProfile>,
        favourites: Set<String>,
    ) {
        appContext.scoutDataStore.edit { preferences ->
            // A corrupt record must survive a replace-all: it is moved into the quarantine bucket rather
            // than cleared with the records that were replaced (E103).
            preserveQuarantined(preferences, SPOT_KEY_PREFIX, QuarantineKind.SPOT)
            preserveQuarantined(preferences, HORIZON_KEY_PREFIX, QuarantineKind.HORIZON)
            clearPrefixed(preferences, SPOT_KEY_PREFIX)
            clearPrefixed(preferences, HORIZON_KEY_PREFIX)
            for (spot in spots) {
                preferences[spotPreferencesKey(spot.id)] = SpotTransfer.encodeSpotRecord(spot)
            }
            for (horizon in horizons) {
                preferences[horizonPreferencesKey(horizon.id)] = SpotTransfer.encodeHorizonRecord(horizon)
            }
            preferences[FAVOURITES_KEY] = SpotTransfer.encodeFavouriteIds(favourites)
            preferences.remove(PRUNED_FAVOURITES_KEY)

            val currentActive = preferences[ACTIVE_SPOT_KEY]?.takeIf { it.isNotBlank() }
            val next = ScoutSelection.reconcile(sortSpots(spots), currentActive)
            if (next == null) preferences.remove(ACTIVE_SPOT_KEY) else preferences[ACTIVE_SPOT_KEY] = next
        }
    }

    /**
     * Drops favourite ids that [knownTargetIds] no longer contains, records them so
     * [ScoutSnapshot.missingTargetRefCount] can report the drop, and returns what was dropped. A dead id
     * is re-written out of the set rather than kept as a permanently dangling entry.
     */
    suspend fun pruneFavourites(knownTargetIds: Set<String>): Set<String> {
        var dropped: Set<String> = emptySet()
        appContext.scoutDataStore.edit { preferences ->
            val current = preferences[FAVOURITES_KEY]
                ?.let { SpotTransfer.decodeFavouriteIds(it) }
                ?: emptySet()
            dropped = current.filterNot { it in knownTargetIds }.toSet()
            val kept = current - dropped
            preferences[FAVOURITES_KEY] = SpotTransfer.encodeFavouriteIds(kept)
            preferences[PRUNED_FAVOURITES_KEY] = SpotTransfer.encodeFavouriteIds(dropped)
        }
        return dropped
    }

    /** The whole scout setup as the export document, named `lumintra-scout-<yyyyMMdd>.json`. */
    suspend fun exportJson(nowUtc: Instant): String {
        val current = snapshot.first()
        return SpotTransfer.encodeDocument(
            spots = current.spots,
            horizons = current.horizons,
            favourites = current.favourites,
            nowUtc = nowUtc,
        )
    }

    /**
     * Imports a document all-or-nothing. The plan is validated with the current store
     * ([SpotTransfer.plan]) **before** anything is written, so a refused document changes nothing.
     *
     * [ImportMode.MERGE] skips a record whose `name` and coordinates match an existing record exactly
     * (`Scout.mergeKey()`) and adds the rest; [ImportMode.REPLACE_ALL] installs the document's spots in
     * place of the saved ones. Favourites always merge as a union and never remove an existing favourite.
     * The active spot is not read from the document — an existing active spot is kept, and one that does
     * not resolve falls back to the first spot after the merge.
     */
    suspend fun importJson(document: String, mode: SpotTransfer.ImportMode): SpotTransfer.ImportResult {
        val current = snapshot.first()
        return when (val plan = SpotTransfer.plan(document, current.spots, current.favourites, mode)) {
            is SpotTransfer.ImportPlan.Refused -> SpotTransfer.ImportResult.Refused(plan.error)

            is SpotTransfer.ImportPlan.Ready -> {
                var activeAfter: String? = null
                appContext.scoutDataStore.edit { preferences ->
                    val existing = readSpotRecords(preferences)
                    if (plan.mode == SpotTransfer.ImportMode.REPLACE_ALL) {
                        // Same durability rule as replaceAll: an unreadable record is kept aside, never
                        // cleared with the records the import replaced (E103).
                        preserveQuarantined(preferences, SPOT_KEY_PREFIX, QuarantineKind.SPOT)
                        preserveQuarantined(preferences, HORIZON_KEY_PREFIX, QuarantineKind.HORIZON)
                        clearPrefixed(preferences, SPOT_KEY_PREFIX)
                        clearPrefixed(preferences, HORIZON_KEY_PREFIX)
                    }
                    for (spot in plan.spots) {
                        preferences[spotPreferencesKey(spot.id)] = SpotTransfer.encodeSpotRecord(spot)
                    }
                    for (horizon in plan.horizons.values) {
                        preferences[horizonPreferencesKey(horizon.id)] =
                            SpotTransfer.encodeHorizonRecord(horizon)
                    }
                    preferences[FAVOURITES_KEY] = SpotTransfer.encodeFavouriteIds(plan.favourites)
                    preferences.remove(PRUNED_FAVOURITES_KEY)

                    val finalSpots = when (plan.mode) {
                        SpotTransfer.ImportMode.REPLACE_ALL -> plan.spots
                        // A merge keeps the saved spots and adds the document's; the document wins on an
                        // id collision so the written record and the reconciled list agree.
                        SpotTransfer.ImportMode.MERGE -> (plan.spots + existing).distinctBy { it.id }
                    }
                    val currentActive = preferences[ACTIVE_SPOT_KEY]?.takeIf { it.isNotBlank() }
                    val resolved = ScoutSelection.reconcile(sortSpots(finalSpots), currentActive)
                    if (resolved == null) preferences.remove(ACTIVE_SPOT_KEY)
                    else preferences[ACTIVE_SPOT_KEY] = resolved
                    activeAfter = resolved
                }
                SpotTransfer.ImportResult.Success(
                    mode = plan.mode,
                    added = plan.added,
                    skipped = plan.skipped,
                    favouritesAdded = plan.favouritesAdded,
                    activeSpotId = activeAfter,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Reading
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the whole store from one Preferences map, parsing each record on its own so a single bad
     * record is quarantined rather than costing the store.
     *
     * Two sources feed the quarantine report, and they are the two halves of E103: a record that is
     * **still in place but unreadable** (`preserved = false`), and a record a destructive write already
     * **moved into the quarantine bucket** (`preserved = true`). Neither is ever deleted on read.
     */
    private fun readSnapshot(preferences: Preferences): ScoutSnapshot {
        val quarantined = ArrayList<QuarantinedRecord>()

        val spots = ArrayList<Scout>()
        for ((key, value) in preferences.asMap()) {
            if (!key.name.startsWith(SPOT_KEY_PREFIX)) continue
            val raw = value as? String
            val spot = raw?.let { SpotTransfer.decodeSpotRecord(it) }
            if (spot == null) {
                quarantined += QuarantinedRecord(
                    id = key.name.removePrefix(SPOT_KEY_PREFIX),
                    kind = QuarantineKind.SPOT,
                    preserved = false,
                )
            } else {
                spots.add(spot)
            }
        }

        val horizons = LinkedHashMap<String, HorizonProfile>()
        for ((key, value) in preferences.asMap()) {
            if (!key.name.startsWith(HORIZON_KEY_PREFIX)) continue
            val raw = value as? String
            val profile = raw?.let { SpotTransfer.decodeHorizonRecord(it) }
            if (profile == null) {
                quarantined += QuarantinedRecord(
                    id = key.name.removePrefix(HORIZON_KEY_PREFIX),
                    kind = QuarantineKind.HORIZON,
                    preserved = false,
                )
            } else {
                horizons[profile.id] = profile
            }
        }

        // The bucket: records a replace-all already kept aside. They are reported as quarantined rows too,
        // so moving a record out of its own key never makes the user's data disappear from the list.
        for ((key, _) in preferences.asMap()) {
            if (!key.name.startsWith(QUARANTINE_KEY_PREFIX)) continue
            val original = key.name.removePrefix(QUARANTINE_KEY_PREFIX)
            when {
                original.startsWith(SPOT_KEY_PREFIX) -> quarantined += QuarantinedRecord(
                    id = original.removePrefix(SPOT_KEY_PREFIX),
                    kind = QuarantineKind.SPOT,
                    preserved = true,
                )

                original.startsWith(HORIZON_KEY_PREFIX) -> quarantined += QuarantinedRecord(
                    id = original.removePrefix(HORIZON_KEY_PREFIX),
                    kind = QuarantineKind.HORIZON,
                    preserved = true,
                )
            }
        }

        val favourites = preferences[FAVOURITES_KEY]
            ?.let { SpotTransfer.decodeFavouriteIds(it) }
            ?: emptySet()
        val pruned = preferences[PRUNED_FAVOURITES_KEY]
            ?.let { SpotTransfer.decodeFavouriteIds(it) }
            ?: emptySet()

        val ordered = sortSpots(spots)
        val rawActive = preferences[ACTIVE_SPOT_KEY]?.takeIf { it.isNotBlank() }
        val resolvedActive = ScoutSelection.resolve(ordered, rawActive)?.id

        return ScoutSnapshot(
            spots = ordered,
            horizons = horizons,
            activeSpotId = resolvedActive,
            favourites = favourites,
            quarantinedCount = quarantined.size,
            missingTargetRefCount = pruned.size,
            quarantined = quarantined,
        )
    }

    /** The parseable spot records only; a quarantined record is skipped, so it can never be active. */
    private fun readSpotRecords(preferences: Preferences): List<Scout> {
        val out = ArrayList<Scout>()
        for ((key, value) in preferences.asMap()) {
            if (!key.name.startsWith(SPOT_KEY_PREFIX)) continue
            val raw = value as? String ?: continue
            SpotTransfer.decodeSpotRecord(raw)?.let { out.add(it) }
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** The deterministic list order: name case-insensitively, then id ascending. */
    private fun sortSpots(spots: List<Scout>): List<Scout> =
        spots.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Scout::name).thenBy(Scout::id))

    /** Removes every preference whose key starts with [prefix]; the keys are collected before removal. */
    private fun clearPrefixed(preferences: MutablePreferences, prefix: String) {
        val doomed = preferences.asMap().keys.filter { it.name.startsWith(prefix) }
        for (key in doomed) preferences.remove(key)
    }

    /**
     * Moves every unreadable `<prefix>*` record into the quarantine bucket **before** a write that is
     * about to clear those keys, so a destructive write can never silently discard a corrupt record
     * (E103). A record that still parses is left where it is — it is about to be replaced by design, not
     * quarantined. This is idempotent: a record already in the bucket has no `<prefix>` key to move.
     */
    private fun preserveQuarantined(
        preferences: MutablePreferences,
        prefix: String,
        kind: QuarantineKind,
    ) {
        val failing = preferences.asMap().entries.filter { (key, value) ->
            if (!key.name.startsWith(prefix)) return@filter false
            val raw = value as? String ?: return@filter true
            when (kind) {
                QuarantineKind.SPOT -> SpotTransfer.decodeSpotRecord(raw) == null
                QuarantineKind.HORIZON -> SpotTransfer.decodeHorizonRecord(raw) == null
            }
        }
        for ((key, value) in failing) {
            val raw = value as? String
            if (raw != null) {
                preferences[stringPreferencesKey(QUARANTINE_KEY_PREFIX + key.name)] = raw
            }
            preferences.remove(key)
        }
    }
}
